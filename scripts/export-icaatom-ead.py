#!/usr/bin/env python

from __future__ import print_function

import os
import sys
import mechanize
import cookielib
from BeautifulSoup import BeautifulSoup

try:
    url, email, password, outdir = sys.argv[1:5]
except Exception:
    print("usage: %s <url> <login-email> <login-password> <output-dir>" % sys.argv[0], file=sys.stderr)
    sys.exit(1)

br = mechanize.Browser()
cj = cookielib.LWPCookieJar()
br.set_cookiejar(cj)

# Browser options
br.set_handle_equiv(True)
br.set_handle_redirect(True)
br.set_handle_referer(True)
br.set_handle_robots(False)

# Follows refresh 0 but not hangs on refresh > 0
br.set_handle_refresh(mechanize._http.HTTPRefreshProcessor(), max_time=1)

# Login
loginpage = br.open(url + "/index.php/;user/login")
br.select_form(nr=1) # the second form - first is search
br.form['email'] = email
br.form['password'] = password
br.submit()

page = br.open(url + "/index.php/;informationobject/browse?limit=2000")
html = page.read()
soup = BeautifulSoup(html)
for td in soup("table", {"class":"sticky-enabled"})[0].tbody("tr"):
    rawpath = td("a")[0]["href"]
    xmlpath = rawpath.replace(";isad", ";ead?sf_format=xml")
    name = os.path.basename(rawpath.replace(";isad", ""))
    try:
        ead = br.open(xmlpath)
        with open(os.path.join(outdir, name + ".xml"), "w") as f:
            f.write(ead.read())
            print(url + xmlpath)
    except Exception, e:
        print(e, file=sys.stderr)



