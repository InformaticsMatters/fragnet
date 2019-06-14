#! /usr/bin/env python

"""A Python wrapper around the Informatics Matters Fragnet service REST API.

Workflow is to create a FragnetSearch instance, authenticate
and then use the FragnetSearch query methods, like search_neighbourhood(): -

    fs = FragnetSearch(fragnet_url, username, password)
    fs.authenticate()
    fs.search_neighbourhood(...)

Authentication obtains a REST API token from the Squonk server.
The token is automatically renewed so, for the lifetime of your FragnetSearch
object you should only need to authenticate once before using any
FragnetSearch methods.

Note:   We do not use the Python logging framework
        due to an issue with Tim's MacBook.
        Instead we use print statements and debug(), warning()
        and error() functions.
"""

import datetime
import os
import pprint
import sys
import urllib

from collections import namedtuple

import requests

# The version of this module.
# Modify with every change, complying with
# semantic 2.0.0 rules.
__version__ = '1.2.0'

# The search result.
# A namedtuple.
SearchResult = namedtuple('SearchResult', 'status_code message json')

# Set to DEBUG
DEBUG = False


def debug(msg):
    """Prints a message (if DEBUG is True)"""
    if DEBUG:
        print('DEBUG: {}'.format(msg))


def warning(msg):
    print('WARNING: {}'.format(msg))


def error(msg):
    print('ERROR: {}'.format(msg))


class FragnetSearchException(Exception):
    """A basic exception used by the FragnetSearch class.
    """
    pass


class FragnetSearch:
    """The FragnetSearch REST API wrapper class.

    Provides convenient and auto-refreshed token-based access to
    the Fragnet REST API.
    """

    SUPPORTED_CALCULATIONS = ['LOGP',
                              'TPSA',
                              'SIM_RDKIT_TANIMOTO',
                              'SIM_MORGAN2_TANIMOTO',
                              'SIM_MORGAN3_TANIMOTO']
    MAX_LIMIT = 5000
    MAX_HOPS = 2

    INTERNAL_ERROR_CODE = 600

    TOKEN_URI = 'https://squonk.it/auth/realms/squonk/protocol/openid-connect/token'
    API = 'fragnet-search/rest/v2'
    CLIENT_ID = 'fragnet-search'
    REQUEST_TIMOUT_S = 20

    # The minimum remaining life of a token (or refresh token) (in seconds)
    # before an automatic refresh is triggered.
    TOKEN_REFRESH_DEADLINE_S = datetime.timedelta(seconds=45)

    def __init__(self, fragnet_host, username, password):
        """Initialises the FragnetSearch module.
        An API token is collected when you 'authenticate'.

        :param fragnet_host: The Fragnet host and designated port,
                             i.e. http://fragnet.squonk.it:8080
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

        debug('fragnet={} username={}'.format(self._fragnet_host,
                                              self._username))

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
            error('access_token is not in the json')
            return False
        if 'expires_in' not in json:
            error('expires_in is not in the json')
            return False
        if 'refresh_token' not in json:
            error('refresh_token is not in the json')
            return False

        # The refresh token may not have an expiry...
        if 'refresh_expires_in' not in json:
            debug('refresh_expires_in is not in the json')

        time_now = datetime.datetime.now()
        self._access_token =json['access_token']
        self._access_token_expiry = time_now + \
                                    datetime.timedelta(seconds=json['expires_in'])
        self._refresh_token = json['refresh_token']
        if 'refresh_expires_in' in json:
            self._refresh_token_expiry = time_now + \
                                         datetime.timedelta(seconds=json['refresh_expires_in'])
        else:
            debug('Setting _refresh_expires_in to None (no refresh expiry)...')
            self._refresh_token_expiry = None

        debug('_access_token_expiry={}'.format(self._access_token_expiry))
        debug('_refresh_token_expiry={}'.format(self._refresh_token_expiry))

        # OK if we get here...
        return True

    def _get_new_token(self):
        """Gets a (new) API access token.
        """
        debug('Getting a new access token...')

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
            warning('_get_new_token() POST timeout')
            return False

        if resp.status_code != 200:
            warning('_get_new_token() resp.status_code={}'.format(resp.status_code))
            return False

        # Get the tokens from the response...
        debug('Got token.')
        return self._extract_tokens(resp.json())

    def _refresh_existing_token(self):
        """Refreshes an (existing) API access token.
        """
        debug('Refreshing the existing access token...')

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
            warning('_refresh_existing_token() POST timeout')
            return False

        if resp.status_code != 200:
            warning('_refresh_existing_token() resp.status_code={}'.format(resp.status_code))
            return False

        # Get the tokens from the response...
        debug('Refreshed token.')
        return self._extract_tokens(resp.json())

    def _check_token(self):
        """Refreshes the access token if it's close to expiry.
        (i.e. if it's within the refresh period). If the refresh token
        is about to expire (i.e. there's been a long time between searches)
        then we get a new token.

        :returns: False if the token could not be refreshed.
        """
        debug('Checking token...')

        time_now = datetime.datetime.now()
        remaining_token_time = self._access_token_expiry - time_now
        if remaining_token_time >= FragnetSearch.TOKEN_REFRESH_DEADLINE_S:
            # Token's got plenty of time left to live.
            # No need to refresh or get a new token.
            debug('Token still has plenty of life remaining.')
            return True

        # If the refresh token is still 'young' (or has no expiry time)
        # then we can rely on refreshing the existing token using it. Otherwise
        # we should collect a whole new token...
        #
        # We set the reaming to me to the limit (which means we'll refresh)
        # but we replace that with any remaining time in the refresh token).
        # So - if there is not expiry time for the refresh token then
        # we always refresh.
        remaining_refresh_time = FragnetSearch.TOKEN_REFRESH_DEADLINE_S
        if self._refresh_token_expiry:
            remaining_refresh_time = self._refresh_token_expiry - time_now
        if remaining_refresh_time >= FragnetSearch.TOKEN_REFRESH_DEADLINE_S:
            # We should be able to refresh the existing token...
            debug('Token too old, refreshing...')
            status = self._refresh_existing_token()
        else:
            # The refresh token is too old,
            # we need to get a new token...
            debug('Refresh token too old, getting a new token...')
            status = self._get_new_token()

        # Return status (success or failure)
        if status:
            debug('Got new token.')
        return status

    def authenticate(self):
        """Authenticates against the server provided in the class initialiser.
        Here we obtain a fresh access and refresh token.

        :returns: True on success

        :raises: FragnetSearchException on error
        """
        debug('Authenticating...')

        status = self._get_new_token()
        if not status:
            raise FragnetSearchException('Unsuccessful Authentication')

        debug('Authenticated.')

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

        :returns: A SearchResult namedtuple consisting of the API 'status_code'
                  (normally 200 on success), a 'message',
                  and the response 'json' content
                  (or None if there is no content).
        """
        # Sanity check the arguments...
        if not smiles.strip():
            return SearchResult(FragnetSearch.INTERNAL_ERROR_CODE,
                                'EmptySmiles', None)
        if hac < 0:
            return SearchResult(FragnetSearch.INTERNAL_ERROR_CODE,
                                'InvalidHAC', None)
        if rac < 0:
            return SearchResult(FragnetSearch.INTERNAL_ERROR_CODE,
                                'InvalidRAC', None)
        if hops < 1 or hops > FragnetSearch.MAX_HOPS:
            return SearchResult(FragnetSearch.INTERNAL_ERROR_CODE,
                                'InvalidHops (%s)' % hops,  None)
        if limit < 1 or limit > FragnetSearch.MAX_LIMIT:
            return SearchResult(FragnetSearch.INTERNAL_ERROR_CODE,
                                'InvalidLimit (%s)' % limit, None)
        if calculations:
            for calculation in calculations:
                if calculation not in FragnetSearch.SUPPORTED_CALCULATIONS:
                    return SearchResult(FragnetSearch.INTERNAL_ERROR_CODE,
                                        'InvalidCalculation: %s' % calculation,
                                        None)

        # Always try to refresh the access token.
        # The token is only refreshed if it is close to expiry.
        if not self._check_token():
            return SearchResult(FragnetSearch.INTERNAL_ERROR_CODE,
                                'APITokenRefreshFailure', None)

        # Construct the basic URI, which includes the URL-encoded
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
        debug('Calling search/neighbourhood for {}...'.format(smiles))
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
            debug('Returned from search with content.')
        except ValueError:
            # No json content.
            # Nothing to do - content is already 'None'
            warning('ValueError getting response json content.')
            pass

        return SearchResult(resp.status_code, 'Success', content)

    def search_suppliers(self):
        """Runs a 'search/suppliers' query on the Fragnet server.

        :returns: A SearchResult namedtuple consisting of the API 'status_code'
                  (normally 200 on success), a 'message',
                  and the response 'json' content
                  (or None if there is no content). The content is
                  a possibly empty list of supplier name strings.
        """
        # Always try to refresh the access token.
        # The token is only refreshed if it is close to expiry.
        if not self._check_token():
            return SearchResult(FragnetSearch.INTERNAL_ERROR_CODE,
                                'APITokenRefreshFailure', None)

        # Construct the basic URI, which includes the URL-encoded
        # version of the provided SMILES string...
        search_uri = '{}/{}/search/suppliers'.\
            format(self._fragnet_host, FragnetSearch.API)

        headers = {'Authorization': 'bearer {}'.format(self._access_token)}

        # Make the request...
        debug('Calling search/suppliers...')
        try:
            resp = requests.get(search_uri,
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
            debug('Returned from search with content.')
        except ValueError:
            # No json content.
            # Nothing to do - content is already 'None'
            warning('ValueError getting response json content.')
            pass

        # Map content to a list of names.
        # The input's a dictionary of labels and names:
        #
        # [{ u'label': u'V_MP', u'name': u'MolPort'},
        #  { u'label': u'V_EMOLS_BB', u'name': u'eMolecules-BB'}]
        #
        translated_content = list(map(lambda x: x['name'], content))

        return SearchResult(resp.status_code, 'Success', translated_content)


# -----------------------------------------------------------------------------
# MAIN
# -----------------------------------------------------------------------------

if __name__ == '__main__':

    fragnet_username = os.environ.get('FRAGNET_USERNAME', None)
    if not fragnet_username:
        print('You need to set FRAGNET_USERNAME')
        sys.exit(1)
    fragnet_password = os.environ.get('FRAGNET_PASSWORD', None)
    if not fragnet_password:
        print('You need to set FRAGNET_PASSWORD')
        sys.exit(1)

    # Pretty-printer
    pp = pprint.PrettyPrinter(indent=2)

    # Create a Fragnet object
    # then authenticate (checking for success)...
    fs = FragnetSearch('http://fragnet.squonk.it:8080',
                       fragnet_username, fragnet_password)
    fs.authenticate()

    # Now run a search for suppliers...
    #
    # The response code from the server is followed by the JSON content
    # The JSON value is 'None' if there is no JSON content.
    result = fs.search_suppliers()

    pp.pprint(result.status_code)
    pp.pprint(result.message)
    pp.pprint(result.json)

    # Now run a basic search...
    hac = 3
    rac = 1
    hops = 2
    limit = 10
    calculations = ['LOGP', 'SIM_RDKIT_TANIMOTO']
    result = fs.search_neighbourhood('c1ccc(Nc2nc3ccccc3o2)cc1',
                                     hac, rac, hops, limit,
                                     calculations)

    pp.pprint(result.status_code)
    pp.pprint(result.message)
    pp.pprint(result.json)
