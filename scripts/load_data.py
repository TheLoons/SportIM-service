# Load data 

import argparse, csv
import json, requests

def parse_args():
    # Command line arguments
    parser = argparse.ArgumentParser()
    parser.add_argument('-sh', '--host', help='service host')
    parser.add_argument('-p', '--port', default=8080, help='service port')
    parser.add_argument('-T', '--teamfile', help='team csv file')
    parser.add_argument('-P', '--playerfile', help='player csv file')
    return parser.parse_args()

def add_player(player, resturl):
    if player['login'] == 'auto':
        player['login'] = player['firstName'][0] + player['lastName'] + '@' + player['team'] + 'sportim.com'
        player['login'] = player['login'].replace(' ', '')
        player['login'] = player['login'].lower()
    if player['password'] == 'auto':
        player['password'] = player['lastName'].lower() + '123'
    if player['phone'] == 'auto':
        player['phone'] = '1234567890'
    data = json.dumps(player)
    resp = requests.request('POST', resturl, data=data, headers={'Content-Type':'application/json'})
    resp = resp.json()
    if (resp['status']['code'] != 200):
        print('Failed to add user (' + str(player) + '). Message: ' + resp['status']['message'])
    return player['login']

def load_players(player_filename, resturl):
    login_to_team = {}
    with open(player_filename, 'rb') as playerfile:
        reader = csv.reader(playerfile, delimiter=',', quotechar='"')
        first = True
        for row in reader:
            if first:
                first = False
                continue
            player = {}
            player['firstName'] = row[0]
            player['lastName'] = row[1]
            player['phone'] = row[2]
            player['password'] = row[3]
            player['team'] = row[4]
            player['login'] = row[5]
            login = add_player(player, resturl + '/user')
            if player['team'] != '':
                login_to_team[login] = player['team']
    return login_to_team

def login(login, passwd, resturl):
    url = resturl + '/login'
    data = json.dumps({'login':login, 'password':passwd})
    resp = requests.request('POST', url, data=data, headers={'Content-Type':'application/json'}).json()
    if (int(resp['status']['code']) != 200):
        print('Failed to add login as ' + login)
        return ''
    return resp['token']

def add_team(team, resturl, token):
    data = json.dumps(team)
    resp = requests.request('POST', resturl, data=data, headers={'Content-Type':'application/json', 'token':token}).json()
    if (int(resp['status']['code']) != 200):
        print('Failed to add team: ' + team['name'])
        return 0
    return int(resp['id'])

def load_teams(team_filename, resturl):
    team_to_id = {}
    with open(team_filename, 'rb') as teamfile:
        reader = csv.reader(teamfile, delimiter=',', quotechar='"')
        first = True
        team_to_id = {}
        token = ''
        lastloggedin = ''
        for row in reader:
            if first:
                first = False
                continue
            team = {}
            team['name'] = row[0]
            if row[2] != lastloggedin:
                token = login(row[2], row[3], resturl)
            team['sport'] = 'soccer'
            team_to_id[team['name']] = add_team(team, resturl + '/team', token);
    return team_to_id, token

def add_player_to_team(login, teamid, token, resturl):
    url = resturl + '/team/' + str(teamid) + '/add?login=' + login
    resp = requests.request('PUT', url, headers={'Content-Type':'application/json', 'token':token}).json()
    if (int(resp['status']['code']) != 200):
        print('Failed to add player ' + login + ' to team ' + str(teamid))

def add_players_to_teams(login_to_team, team_to_id, resturl, token):
    for login in login_to_team:
        add_player_to_team(login, team_to_id[login_to_team[login]], resturl, token)

def main():
    args = parse_args()
    resturl = 'http://' + args.host + ':' + str(args.port) + '/rest'
    print('Adding players...')
    login_to_team = load_players(args.playerfile, resturl)
    print('Adding teams...')
    team_to_id, token = load_teams(args.teamfile, resturl)
    print('Adding players to teams...')
    add_players_to_teams(login_to_team, team_to_id, token, resturl)
    print('Done')

if __name__ == "__main__":
    main()
