#! /usr/bin/env python

import datetime
import os
import pprint
import urllib

from collections import namedtuple

import requests

pp = pprint.PrettyPrinter(indent=2)

# The search result.
# A namedtuple.
SearchResult = namedtuple('SearchResult', 'status_code message json')

class FragnetSearchException(Exception):
    pass

class FragnetSearch:
    """The FragnetSearch REST API wrapper class.

    Provides convenient and auto-refreshed token-based access to
    the Fragnet REST API.
    """

    SUPPORTED_CALCULATIONS = ['LOGP',
                              'TPSA ',
                              'SIM_RDKIT_TANIMOTO',
                              'SIM_MORGAN2_TANIMOTO',
                              'SIM_MORGAN3_TANIMOTO']
    MAX_LIMIT = 5000
    MAX_HOPS = 2

    INTERNAL_ERROR_CODE = 600

    TOKEN_URI = 'https://squonk.it/auth/realms/squonk/protocol/openid-connect/token'
    API = 'fragnet-search/rest/v1'
    CLIENT_ID = 'fragnet-search'
    REQUEST_TIMOUT_S = 10

    # The minimum remaining life of a token (or refresh token) (in seconds)
    # before an automatic refresh is triggered.
    TOKEN_REFRESH_S = datetime.timedelta(seconds=60)

    def __init__(self, fragnet_host, username, password):
        """Initialises the FragnetSearch module.
        An API token is collected when you 'authenticate'.

        :param fragnet_host: The Fragnet host and designated port,
                             i.e. http://fragnet.squonk.ot:8080
        :type fragnet_host: ``str``
        :param username: A Fragnet username
        :type username: ``str``
        :param password: The Fragnet user's password
        :type password: ``str``
        """
        # We do nothing other then record parameters
        # to be used elsewhere in the class...
        self._username = username
        self._password = password
        self._fragnet_host = fragnet_host

        self._access_token = None
        self._access_token_expiry = None
        self._refresh_token = None
        self._refresh_token_expiry = None

    def _extract_tokens(self, json):
        """Gets tokens from a valid json response.
        The JSON is expected to contain an 'access_token',
        'refresh_token', 'expires_in' and 'refresh_expires_in'.

        We calculate the token expiry times here,
        which is used by '_check_token()' to automatically
        renew the token.

        :param json: The JSON payload, containing tokens
        """
        if 'access_token' not in json:
            return False
        if 'expires_in' not in json:
            return False
        if 'refresh_token' not in json:
            return False
        if 'refresh_expires_in' not in json:
            return False

        time_now = datetime.datetime.now()
        self._access_token =json['access_token']
        self._access_token_expiry = time_now + \
            datetime.timedelta(seconds=json['expires_in'])
        self._refresh_token = json['refresh_token']
        self._refresh_token_expiry = time_now + \
            datetime.timedelta(seconds=json['refresh_expires_in'])

        # OK if we get here...
        return True

    def _get_token(self):
        """Gets a (new) API access token.
        """
        headers = {'Content-Type': 'application/x-www-form-urlencoded'}
        payload = {'grant_type': 'password',
                   'client_id': FragnetSearch.CLIENT_ID,
                   'username': self._username,
                   'password': self._password}
        try:
            resp = requests.post(FragnetSearch.TOKEN_URI,
                                 data=payload,
                                 headers=headers,
                                 timeout=FragnetSearch.REQUEST_TIMOUT_S)
        except requests.exceptions.ConnectTimeout:
            return False

        if resp.status_code != 200:
            return False

        # Get the tokens from the response...
        return self._extract_tokens(resp.json())

    def _refresh_token(self):
        """Refreshes an (existing) API access token.
        """
        headers = {'Content-Type': 'application/x-www-form-urlencoded'}
        payload = {'grant_type': 'refresh_token',
                   'client_id': FragnetSearch.CLIENT_ID,
                   'refresh_token': self._refresh_token}
        try:
            resp = requests.post(FragnetSearch.TOKEN_URI,
                                 data=payload,
                                 headers=headers,
                                 timeout=FragnetSearch.REQUEST_TIMOUT_S)
        except requests.exceptions.ConnectTimeout:
            return False

        if resp.status_code != 200:
            return False

        # Get the tokens from the response...
        return self._extract_tokens(resp.json())

    def _check_token(self):
        """Refreshes the access token if it's close to expiry.
        (i.e. if it's within the refresh period). If the refresh token
        is about to expire (i.e. there's been a long time between searches)
        then we get a new token.

        :returns: False if the token could not be refreshed.
        """
        time_now = datetime.datetime.now()
        remaining_token_time = self._access_token_expiry - time_now
        if remaining_token_time > FragnetSearch.TOKEN_REFRESH_S:
            # Token's got plenty of time left to live.
            # Nothing to do...
            return True

        # If the refresh token is still young then we can
        # rely on refreshing the existing token. Otherwise
        # we should collect a new token...
        remaining_refresh_time = self._refresh_token_expiry - time_now
        if remaining_refresh_time > FragnetSearch.TOKEN_REFRESH_S:
            # We should be able to refresh the existing token
            # (and get a new refresh token).
            status = self._refresh_token()
        else:
            # The refresh token is too old,
            # we need to get a new token...
            status = self._get_token()

        # Return status (success or failure)
        return status

    def authenticate(self):
        """Authenticates against the server provided in the class initialiser.
        Here we obtain a fresh access and refresh token.

        :returns: True on success

        :raises: FragnetSearchException on error
        """
        status = self._get_token()
        if not status:
            raise FragnetSearchException('Unsuccessful Authentication')

    def search_neighbourhood(self, smiles, hac, rac, hops, limit, calculations):
        """Runs a 'search/neighbourhood' query on the Fragnet server.
        The API token is collected when this class instance has been
        created.

        :param smiles: The SMILES string on which th base the search,
                       This will be URL-encoded bnu the method so you provide
                       the 'raw' smiles.
        :type smiles: ``str``
        :param hac: Heavy Atom Count.
        :type hac: ``int``
        :param rac: Ring Atom Count.
        :type rac: ``int``
        :param hops: Hops (1 or 2)
        :type hops: ``int``
        :param limit: Search limit.
        :type limit: ``int``
        :param calculations: The list of calculations (can be empty)
        :type calculations: ``list``
        :returns: A tuple consisting of the GET status code
                  (normally 200 on success), a message,
                  and the response JSON content (or None).
        """
        # Sanity check arguments...
        if not smiles.strip():
            return SearchResult(FragnetSearch.INTERNAL_ERROR_CODE,
                                'EmptySmiles', None)
        if hac < 1:
            return SearchResult(FragnetSearch.INTERNAL_ERROR_CODE,
                                'InvalidHAC', None)
        if rac < 1:
            return SearchResult(FragnetSearch.INTERNAL_ERROR_CODE,
                                'InvalidRAC', None)
        if hops < 1 or hops > FragnetSearch.MAX_HOPS:
            return SearchResult(FragnetSearch.INTERNAL_ERROR_CODE,
                                'InvalidHops', None)
        if limit < 1 or limit > FragnetSearch.MAX_LIMIT:
            return SearchResult(FragnetSearch.INTERNAL_ERROR_CODE,
                                'InvalidLimit', None)
        if calculations:
            for calculation in calculations:
                if calculation not in FragnetSearch.SUPPORTED_CALCULATIONS:
                    return SearchResult(FragnetSearch.INTERNAL_ERROR_CODE,
                                        'InvalidCalculation', None)

        # Always try to refresh the access token.
        # The token is only refreshed if it is close to expiry.
        if not self._check_token():
            return SearchResult(FragnetSearch.INTERNAL_ERROR_CODE,
                                'APITokenRefreshFailure', None)

        # Construct the basic URI, whcih includes the URL-encoded
        # version of the provided SMILES string...
        search_uri = '{}/{}/search/neighbourhood/{}'.\
            format(self._fragnet_host,
                   FragnetSearch.API,
                   urllib.quote(smiles))

        headers = {'Authorization': 'bearer {}'.format(self._access_token)}
        params = {'hac': hac,
                  'rac': rac,
                  'hops': hops,
                  'limit': limit}
        if calculations:
            params['calcs'] = ','.join(calculations)

        # Make the request...
        try:
            resp = requests.get(search_uri,
                                params=params,
                                headers=headers,
                                timeout=FragnetSearch.REQUEST_TIMOUT_S)
        except requests.exceptions.ConnectTimeout:
            return SearchResult(FragnetSearch.INTERNAL_ERROR_CODE,
                                'RequestTimeout', None)

        # Try to extract the JSON content
        # and return it and the status code.
        content = None
        try:
            content = resp.json()
        except ValueError:
            # No json content.
            # Nothing to do - content is already 'None'
            pass

        return SearchResult(resp.status_code, 'Success', content)

if __name__ == '__main__':

    fragnet_username = os.environ['FRAGNET_USERNAME']
    fragnet_password = os.environ['FRAGNET_PASSWORD']

    # Create a Fragnet object
    # then authenticate (checking for success)...
    fs = FragnetSearch('http://fragnet.squonk.it:8080',
                       fragnet_username, fragnet_password)
    fs.authenticate()

    # Now run a basic search.
    # The response code from the server is followed by the JSON content
    # The JSON value is 'None' if there is no JSON content.
    hac = 3
    rac = 1
    hops = 2
    limit = 10
    calculations = ['LOGP', 'SIM_RDKIT_TANIMOTO']
    result = fs.search_neighbourhood('c1ccc(Nc2nc3ccccc3o2)cc1',
                                     hac, rac, hops, limit,
                                     calculations)

    # Pretty-print a summary...
    pp.pprint(result.status_code)
    pp.pprint(result.message)
    pp.pprint(result.json)
