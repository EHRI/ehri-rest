#!/bin/bash

# Nieuw stappenplan :)

# Zorg dat code bouwt en werkt
# gebruik fab git_tag om te taggen
# Commit naar publieke master


# Pull code op ehritest
# Bouw op ehritest
# Verplaats JARs naar staging



# VOOR NU: BOUW LOKAAL

fab stage clean_deploy

# Zorg dat je je data op de juiste plek hebt gezet:
# op staging (en productie?): /home/benc/import-data/<country code>/<repository name>
# e.g. /home/benc/import-data/nl/niod

# Zorg ervoor dat lib.sh up to date is.
# It's not necessary to do this on every deployment.

# fab stage copy_lib_sh 

# Online backup maken van productiedatabase
# kopieer naar lokale machine als tar
# kopieer naar staging als tar
# geen online backup maken van staging
# 'oude' database op staging weggooien
# productiedatabase op staging uitpakken en op de plek zetten

fab prod online_clone_db:/tmp/cloned_db
fab stage update_db:/tmp/cloned_db




# voer imports uit

