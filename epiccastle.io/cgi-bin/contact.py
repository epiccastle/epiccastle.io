#!/usr/bin/env python

import cgi
import sys
import smtplib
import time
import os
import errno
from email.mime.text import MIMEText

def mkdir_p(path):
    try:
        os.makedirs(path)
    except OSError as exc:  # Python >2.5
        if exc.errno == errno.EEXIST and os.path.isdir(path):
            pass
        else:
            raise

#import cgitb; cgitb.enable()  # for troubleshooting

storage = os.environ.get('CONTACT_STORAGE', './storage/')
timestamp = time.time()
store_file = os.path.join(storage, "%f.txt"%timestamp)

mkdir_p(storage)

form = cgi.FieldStorage()
name = form.getvalue("name")
email = form.getvalue("email")
message = form.getvalue("message")

with open(store_file, 'w') as fh:
    fh.write("name: %s\n"%name);
    fh.write("email: %s\n"%email);
    fh.write("message: %s\n"%message);

# email
msg = MIMEText("""
Recieved message on epiccastle.io contact form page
===================================================
name: %s
email: %s
message: %s
"""%(name, email, message))

me = 'Epic Castle Website <noreply@epiccastle.io>'
to = 'Crispin Wellington <crispin@epiccastle.io>'
cc = None

msg['Subject'] = 'epiccastle.io contact form has a submission'
msg['From'] = me
msg['To'] = to
if cc:
    msg['Cc'] = cc

conn = smtplib.SMTP('localhost')
conn.sendmail(me, [to], msg.as_string())
conn.quit()

print "Status-code: 200"
print
