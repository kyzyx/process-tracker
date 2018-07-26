# Process Tracker

## Overview
This app allows you to track the process of long-running jobs running on
a remote server, such as:
- Transferring large files
- Training deep neural networks
- Compiling large programs
- Batch file operations

There are two components to this app: a python server running on the machine that is executing the jobs, and an Android app to track the updates. Communication
is mediated by Google Sheets.

TODO: A smartwatch app

## Setup
### Google Drive Setup
1. Go to https://console.developers.google.com/projectcreate and enter a new project name
2. Navigate to the new project page
3. Go to https://console.developers.google.com/apis/library/drive.googleapis.comand click "Enable"
3. Go to https://console.developers.google.com/apis/library/sheets.googleapis.comand click "Enable"
4. Go to https://console.developers.google.com/apis/credentials/serviceaccountkey and download a JSON key
5. Move that key to server/auth.json

### Python server setup
Install dependencies:
> $ pip install gspread oauth2client

Run server (point at logs directory)
> $ python server/server.py logs/

FIXME: User account (need email) vs public sheet

FIXME: How to get sheet location to android app?

### Android app setup
FIXME

## Rich output
By default, the app will simply display the last N lines of output from each job. However, it will also display more useful information if it detects it.
- Progress percentage: If a percent symbol is immediately preceded by a number, progress bar mode is automatically enabled
- Progress fraction: If one or more fractions (number / number) are found, then they are treated as progress fractions, with finer resolution progress to the right. Progress bar mode is automatically enabled if the fractions are the first tokens in the line.
- Error: If the word "Error" or "Fatal" is detected, a notification is sent.
- Graph: A graph can be enabled for a particular token in the output.
- Update the status of the task by outputting a line beginning with "Status:"
- Update the displayed task title by outputting a line beginning with "Task:"
