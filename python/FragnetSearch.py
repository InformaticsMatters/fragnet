#! /usr/bin/env python

"""A Python wrapper around the Informatics Matters Fragnet service REST API.

Workflow is to create a FragnetSearch instance, authenticate
and then use the FragnetSearch query methods, like search_neighbourhood(): -

    fs = FragnetSearch(fragnet_url, username, password)
    fs.authenticate()
    fs.search_neighbourhood(...)

Authentication obtains a REST API tokenm from the Squonk server.
The token is automatically renewed so, for the lifetime of your fs
object you only need to authenticate once.
"""

import logging
import logging.config
import datetime
import os
import pprint
import sys
import urllib

from collections import namedtuple

import requests
import yaml

# The search result.
# A namedtuple.
SearchResult = namedtuple('SearchResult', 'status_code message json')


def setup_logging(default_path='logging.yaml',
                  default_level=logging.INFO,
                  env_key='FRAGNET_LOG_CFG'):
    """Setup logging configuration.

    :param default_path: The path and file for logging configuration (YAML)
    :param default_level: The default logging level (A logging level)
    :param env_key: The environment variable that over-rides the default path
    """
    path = default_path
    value = os.getenv(env_key, None)
    if value:
        path = value
    if os.path.exists(path):
        with open(path, 'rt') as f:
            config = yaml.safe_load(f.read())
        logging.config.dictConfig(config)
    else:
        logging.basicConfig(level=default_level)


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
    TOKEN_REFRESH_DEADLINE_S = datetime.timedelta(seconds=45)

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

        self.logger = logging.getLogger('FragnetSearch')

        # We do nothing other then record parameters
        # to be used elsewhere in the class...
        self._username = username
        self._password = password
        self._fragnet_host = fragnet_host

        self._access_token = None
        self._access_token_expiry = None
        self._refresh_token = None
        self._refresh_token_expiry = None

        self.logger.debug('fragnet=%s username=%s',
                          self._fragnet_host, self._username)

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
            self.logger.error('access_token is not in the json')
            return False
        if 'refresh_token' not in json:
            self.logger.error('refresh_token is not in the json')
            return False
        if 'refresh_token' not in json:
            self.logger.error('refresh_token is not in the json')
            return False

        # The refresh token may not have an expiry...
        if 'refresh_expires_in' not in json:
            self.logger.debug('refresh_expires_in is not in the json')

        time_now = datetime.datetime.now()
        self._access_token =json['access_token']
        self._access_token_expiry = time_now + \
            datetime.timedelta(seconds=json['expires_in'])
        self._refresh_token = json['refresh_token']
        if 'refresh_expires_in' in json:
            self._refresh_token_expiry = time_now + \
                datetime.timedelta(seconds=json['refresh_expires_in'])
        else:
            self.logger.debug('Setting _refresh_expires_in to None (no expiry)...')
            self._refresh_token_expiry = None

        self.logger.debug('_access_token_expiry=%s', self._access_token_expiry)
        self.logger.debug('_refresh_token_expiry=%s', self._refresh_token_expiry)

        # OK if we get here...
        return True

    def _get_new_token(self):
        """Gets a (new) API access token.
        """
        self.logger.debug('Getting a new access token...')

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
            self.logger.warning('POST timeout')
            return False

        if resp.status_code != 200:
            self.logger.warning('resp.status_code=%d', resp.status_code)
            return False

        # Get the tokens from the response...
        self.logger.debug('Got token.')
        return self._extract_tokens(resp.json())

    def _refresh_existing_token(self):
        """Refreshes an (existing) API access token.
        """
        self.logger.debug('Refreshing the existing access token...')

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
            self.logger.warning('POST timeout')
            return False

        if resp.status_code != 200:
            self.logger.warning('resp.status_code=%d', resp.status_code)
            return False

        # Get the tokens from the response...
        self.logger.debug('Refreshed token.')
        return self._extract_tokens(resp.json())

    def _check_token(self):
        """Refreshes the access token if it's close to expiry.
        (i.e. if it's within the refresh period). If the refresh token
        is about to expire (i.e. there's been a long time between searches)
        then we get a new token.

        :returns: False if the token could not be refreshed.
        """
        self.logger.debug('Checking token...')

        time_now = datetime.datetime.now()
        remaining_token_time = self._access_token_expiry - time_now
        if remaining_token_time >= FragnetSearch.TOKEN_REFRESH_DEADLINE_S:
            # Token's got plenty of time left to live.
            # No need to refresh or get a new token.
            self.logger.debug('Token still has plenty of life remaining.')
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
            self.logger.debug('Token too old, refreshing...')
            status = self._refresh_existing_token()
        else:
            # The refresh token is too old,
            # we need to get a new token...
            self.logger.debug('Refresh token too old, getting a new token...')
            status = self._get_new_token()

        # Return status (success or failure)
        if status:
            self.logger.debug('Got new token.')
        return status

    def authenticate(self):
        """Authenticates against the server provided in the class initialiser.
        Here we obtain a fresh access and refresh token.

        :returns: True on success

        :raises: FragnetSearchException on error
        """
        self.logger.debug('Authenticating...')

        status = self._get_new_token()
        if not status:
            raise FragnetSearchException('Unsuccessful Authentication')

        self.logger.debug('Authenticated.')

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
        self.logger.debug('Calling search/neighbourhood for %s...', smiles)
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
            self.logger.debug('Returned from search with content.')
        except ValueError:
            # No json content.
            # Nothing to do - content is already 'None'
            self.logger.wanring('ValueError getting response json content.')
            pass

        return SearchResult(resp.status_code, 'Success', content)

# -----------------------------------------------------------------------------
# MAIN
# -----------------------------------------------------------------------------

if __name__ == '__main__':

    setup_logging()

    fragnet_username = os.environ.get('FRAGNET_USERNAME', None)
    if not fragnet_username:
        print('You need to set FRAGNET_USERNAME')
        sys.exit(1)
    fragnet_password = os.environ.get('FRAGNET_PASSWORD', None)
    if not fragnet_password:
        print('You need to set FRAGNET_PASSWORD')
        sys.exit(1)

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
    pp = pprint.PrettyPrinter(indent=2)

    pp.pprint(result.status_code)
    pp.pprint(result.message)
    pp.pprint(result.json)
