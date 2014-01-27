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

# For a first time "user add", create a new user in the portal: "More" -> "Users" -> "Create User". Use the username that will be the identifier of the user profile that you re-add later.
# Add your import command to queue.sh (queue.sh MUST NOT be in the git repository)
# Add the required users to queue.sh (remember? the names that you added via the portal)
# (We assume everyone who has access to the staging server is allowed to view everything that is on the staging server)

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
fab stage load_queue
