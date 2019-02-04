#! /usr/bin/env python

import datetime
import os
import pprint
import sys
import urllib

import requests

pp = pprint.PrettyPrinter(indent=2)

fragnet_username = os.environ['FRAGNET_USERNAME']
fragnet_password = os.environ['FRAGNET_PASSWORD']

class FragnetSearch:

    SUPPORTED_CALCULATIONS = ['LOGP',
                              'TPSA ',
                              'SIM_RDKIT_TANIMOTO',
                              'SIM_MORGAN2_TANIMOTO',
                              'SIM_MORGAN3_TANIMOTO']
    MAX_LIMIT = 5000
    MAX_HOPS = 2

    TOKEN_URI = 'https://squonk.it/auth/realms/squonk/protocol/openid-connect/token'
    API = 'fragnet-search/rest/v1'
    CLIENT_ID = 'fragnet-search'
    REQUEST_TIMOUT_S = 10

    def __init__(self, fragnet_host, username, password):
        """Initialises the FragnetSearch module.
        An API token is collected when you 'authenticate'.

        :param username: A Fragnet username
        :type username: ``str``
        :param password: The Fragnet user's password
        :type password: ``str``
        :param fragnet_host: The Fragnet host and designated port,
                             i.e. http://fragnet.squonk.ot:8080
        :type fragnet_host: ``str``
        """
        self._username = username
        self._password = password
        self._fragnet_host = fragnet_host

        self._access_token = None
        self._access_token_expiry = None
        self._refresh_token = None
        self._refresh_token_expiry = None

    def authenticate(self):
        """Authenticates against the server provided in the class initialiser.
        Here we obtain a fresh access and refresh token.

        :returns: True in success
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

        time_now = datetime.datetime.now()
        self._access_token = resp.json()['access_token']
        self._access_token_expiry = time_now +\
            datetime.timedelta(seconds=resp.json()['expires_in'])
        self._refresh_token = resp.json()['refresh_token']
        self._refresh_token_expiry = time_now + \
            datetime.timedelta(seconds=resp.json()['refresh_expires_in'])

        return True

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
        :param hops: limit.
        :type hops: ``int``
        :param calculations: The list of calculations (can be empty)
        :type calculations: ``list``
        :returns: A tuple consisting of the GET status code
                  (normally 200 on success), a message,
                  and the response JSON content (or None).
        """
        # Sanity check arguments...
        if not smiles.strip():
            return 600, 'EmptySmiles', None
        if hac < 1:
            return 600, 'InvalidHAC', None
        if rac < 1:
            return 600, 'InvalidRAC', None
        if hops < 1 or hops > FragnetSearch.MAX_HOPS:
            return 600, 'InvalidHops', None
        if limit < 1 or limit > FragnetSearch.MAX_LIMIT:
            return 600, 'InvalidLimit', None
        if calculations:
            for calculation in calculations:
                if calculation not in FragnetSearch.SUPPORTED_CALCULATIONS:
                    return 600, 'InvalidCalculation', None

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
            return 600, 'RequestTimeout', None

        # Try to extract the JSON content
        # and return it and the status code.
        content = None
        try:
            content = resp.json()
        except ValueError:
            # No json content.
            # Nothing to do - content is already 'None'
            pass

        return resp.status_code, 'Success', content

if __name__ == '__main__':

    # Create a Fragnet object
    # then authenticate (checking for success)...
    fs = FragnetSearch('http://fragnet.squonk.it:8080',
                       fragnet_username, fragnet_password)
    success = fs.authenticate()
    if not success:
        print('ERROR: Authentication failed')
        print('       Please check your host, username and password')
        sys.exit(1)

    # Now run a basic search.
    # The response code from the server is followed by the JSON content
    # The JSON value is 'None' if there is no JSON content.
    hac = 3
    rac = 1
    hops = 2
    limit = 10
    calculations = ['LOGP', 'SIM_RDKIT_TANIMOTO']
    status, msg, json = fs.search_neighbourhood('c1ccc(Nc2nc3ccccc3o2)cc1',
                                                hac, rac, hops, limit,
                                                calculations)

    # Pretty-print a summary...
    pp.pprint(status)
    pp.pprint(msg)
    pp.pprint(json)
