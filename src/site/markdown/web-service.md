# Using the Web Service

This will be a general walk-through of how to use the web service to create, import, and fetch data. First,
initialise a new Neo4j database as described in the [initialisation instructions](initialise.html). Now,
restart the Neo4j server like do:

```bash
$NEO4J_HOME/bin/neo4j start
```

Verify that everything's working by fetching your user profile using the following Curl command
(for these examples I'll be using my own `$USER`, which is `mike`):

```bash
curl http://localhost:7474/ehri/userProfile/mike
```

If all goes well you should see something like:

    {
      "id" : "mike",
      "type" : "userProfile",
      "data" : {
        "identifier" : "mike",
        "name" : "Mike Bryant"
      },
      "meta" : {
        "gid" : 22
      },
      "relationships" : {
        "lifecycleEvent" : [ {
          "relationships" : {
            "hasActioner" : [ {
              "relationships" : { },
              "data" : {
                "identifier" : "admin",
                "name" : "Admin"
              },
              "meta" : {
                "followers" : 0,
                "watching" : 0,
                "following" : 0
              },
              "id" : "admin",
              "type" : "group"
            } ]
          },
          "data" : {
            "eventType" : "creation",
            "timestamp" : "2015-10-01T15:06:38.411+01:00"
          },
          "meta" : {
            "childCount" : 1
          },
          "id" : "a2035f24-6845-11e5-92aa-ed4cb7d1dda8",
          "type" : "systemEvent"
        } ],
        "belongsTo" : [ {
          "relationships" : { },
          "data" : {
            "identifier" : "admin",
            "name" : "Administrators"
          },
          "meta" : {
            "childCount" : 1
          },
          "id" : "admin",
          "type" : "group"
        } ]
      }
    }
    
This JSON data is a type of graph. It says that:

 - The user profile has global ID `mike`, which is the same as the local (non-hierarchical) identifier `mike`
 - The profile is of type `userProfile`
 - It has a `belongsTo` relationship to a `group` item with ID `admin`
 - There was a `systemEvent` of type `creation`, "actioned" (or initiated) by `admin`.

Assuming that worked, lets go on and create the following:

 1. a `country` item (via the web service RESTful API)
 2. a `repository` item via [EAG](http://www.apex-project.eu/index.php/en/outcomes/standards/eag-2012) XML import
 .de/) XML import)
 3. a `DocumentaryUnit` item via [EAD](http://www.loc.gov/ead/) XML import

Items 1, 2, and 3 here have a hierarchical relationship, which means that we need to create them in order,
since the documentary unit item will "belong" to the repository, and the repository to the country item.

## Web service usage conventions

Before we start, lets just explain a couple of conventions for using the web service. You'll notice below
that any requests that _change_ data, e.g. `POST`, `PUT`, `DELETE` methods, require an `X-User: $USER` header.
This identifier the "actioner" to the system and allows the permission system to determine if they have adequate
permissions (obviously, it's the responsibility of the client application to verify people are who they say they
are, but the web service does not handle authentication, only authorisation.)

We also use the `X-LogMessage` header when we want to tell the system what we're doing. This will end up as the
 `logMessage` property on the `systemEvent` item associated with the change.

## Creating the country

We're going to create the country via the "RESTful" web service API. The only data we absolutely need for
the country item is an ISO-3166-1 alpha-2 (2 letter) code. For now we'll use `nl`, for the Netherlands.

We need to send JSON data to the web service and it needs to be in a particular format, namely an object
with the following data members:

 - a `type` property for the "content type" of the new item
 - a `data` object property, containing at least the item's mandatory properties
 - an optional `relationships` object attribute, for nested data members
 
For a country, we just need `type`, and `data`, and the only attribute that needs to be present in `data`
is the `identifier` property containing our two-letter code. So the JSON data we'll send looks like this:

    {
      "type": "country",
      "data": {
        "identifier": "nl"
      }
    }
    
Let's send that via CURL to the `/ehri/country` endpoint, using `application/json` as the content type:

```bash
curl -X POST \
     -H "X-User: mike" \
     -H "Content-type: application/json" \
     --data '{"type": "country", "data": {"identifier": "nl"}}' \
     http://localhost:7474/ehri/country
```

We should recieve the newly-create item as the response, with more system-created metadata:

    {
      "id" : "nl",
      "data" : {
        "identifier" : "nl"
      },
      "type" : "country",
      "relationships" : {
        "lifecycleEvent" : [ {
          "id" : "1faabf00-6903-11e5-a63f-d32c8c516fe2",
          "data" : {
            "timestamp" : "2015-10-02T13:43:04.105+01:00",
            "eventType" : "creation"
          },
          "type" : "systemEvent",
          "relationships" : {
            "hasActioner" : [ {
              "id" : "mike",
              "data" : {
                "name" : "mike",
                "identifier" : "mike"
              },
              "type" : "userProfile",
              "relationships" : { },
              "meta" : {
                "following" : 0,
                "followers" : 0,
                "watching" : 0
              }
            } ]
          },
          "meta" : {
            "childCount" : 1
          }
        } ]
      },
      "meta" : {
        "childCount" : 0,
        "gid" : 26
      }
    }
    
A few things to note:

 - the system-defined global ID is the same as the local identifier property we provided, since countries
   are top-level items
 - there's a `creation` event, actioned by us
 
Since the global ID is `nl`, we can fetch the item from the web service specifically using:

```bash
curl http://localhost:7474/ehri/country/nl
```

This should give the same result as when we created it.

## Importing a repository via EAG

Now we're going to create a repository inside our new country. Since repositories require somewhat more metadata
to be useful, we're going to import it via an EAG file. Download and extract the sample EAC, EAD, and EAG XML
documents from the [samples.tgz](samples.tgz) archive.

To import `eag.xml` we're going to use the `/ehri/import/eag` web service method like so:

```bash
curl    -X POST \
        -H "X-User: mike" \
        -H "X-LogMessage: Testing EAG import" \
        -H "Content-type: text/xml" \
        --data @eag.xml \
        http://localhost:7474/ehri/import/eag?scope=nl
```

We should receive a response like so:

    {"message":"Testing EAG import","updated":0,"created":1,"unchanged":0}
    
This tells us that we created one new item, as expected. The import endpoint is [idempotent](https://en.wikipedia.org/wiki/Idempotence) 
so we can run the same thing again and it'll tell us that there were no changes.

    {"message":"Testing EAG import","updated":0,"created":0,"unchanged":1}

Unfortunately, the import methods don't tell us much about the actual items we've just created, so lets list
all the repositories and see what we see there:

```bash
curl http://localhost:7474/ehri/repository
```

This gives us the somewhat more extensive data:

    [
        {
            "data": {
                "identifier": "test-repository",
                "typeOfEntity": "organisation"
            },
            "id": "nl-test_repository",
            "meta": {
                "childCount": 0,
                "gid": 30,
                "watchedBy": 0
            },
            "relationships": {
                "describes": [
                    {
                        "data": {
                            "accessibility": "Use public transport",
                            "creationProcess": "IMPORT",
                            "history": "Example Repository Description",
                            "holdings": "Large",
                            "identifier": "test-repository#desc",
                            "languageCode": "eng",
                            "name": "Test Repository",
                            "openingTimes": "9-5 All week",
                            "otherFormsOfName": "Test Repository - Alt Name",
                            "rulesAndConventions": "ISDIAH",
                            "typeOfEntity": "organisation"
                        },
                        "id": "nl-test_repository.eng-test_repository_desc",
                        "meta": {
                            "gid": 31
                        },
                        "relationships": {
                            "hasAddress": [
                                {
                                    "data": {
                                        "countryCode": "NL",
                                        "email": "info@example.com",
                                        "municipality": "A Town",
                                        "postalCode": "ABC 123",
                                        "street": "Any Street",
                                        "telephone": "12345 678910",
                                        "webpage": "www.example.com"
                                    },
                                    "id": "3320f8ec-6905-11e5-accc-dd70c2facf88",
                                    "meta": {
                                        "gid": 33
                                    },
                                    "relationships": {},
                                    "type": "address"
                                }
                            ],
                            "hasUnknownProperty": [
                                {
                                    "data": {
                                        "eag_archguide_desc_email_": "Email",
                                        "eag_archguide_desc_webpage_": "Website",
                                        "eag_archguide_identity_repositorid_": "test"
                                    },
                                    "id": "3320f8ee-6905-11e5-accc-dd70c2facf88",
                                    "meta": {
                                        "gid": 32
                                    },
                                    "relationships": {},
                                    "type": "property"
                                }
                            ]
                        },
                        "type": "repositoryDescription"
                    }
                ],
                "hasCountry": [
                    {
                        "data": {
                            "identifier": "nl"
                        },
                        "id": "nl",
                        "meta": {
                            "childCount": 1
                        },
                        "relationships": {},
                        "type": "country"
                    }
                ],
                "lifecycleEvent": [
                    {
                        "data": {
                            "eventType": "ingest",
                            "timestamp": "2015-10-02T13:57:55.686+01:00"
                        },
                        "id": "334e98a2-6905-11e5-accc-dd70c2facf88",
                        "meta": {
                            "childCount": 1
                        },
                        "relationships": {
                            "hasActioner": [
                                {
                                    "data": {
                                        "identifier": "mike",
                                        "name": "mike"
                                    },
                                    "id": "mike",
                                    "meta": {
                                        "followers": 0,
                                        "following": 0,
                                        "watching": 0
                                    },
                                    "relationships": {},
                                    "type": "userProfile"
                                }
                            ]
                        },
                        "type": "systemEvent"
                    }
                ]
            },
            "type": "repository"
        }
    ]

We have a JSON list with one item (as expected, since we've only created one repository). A few things to note:

 - this time the `lifecycleEvent` is `ingest`, again with `hasActioner` "mike".
 - there is a `hasCountry` relationship pointing to our `nl` country
 - there is a `describes` relationship to a `repositoryDescription` item with (default)
   `languageCode` value `eng`, that contains the bulk of the data in our EAG file.
 - the system ID is `nl-test_repository`, and the identifier value is `test_repository`.
 
We can fetch the item directly using its ID via:
 
```bash
curl http://localhost:7474/ehri/repository/nl-test_repository
```

## Importing a documentary unit via EAD

Importing EAD is much the same process, only this time we use the `/ehri/import/ead` method and
the repository (with ID `nl-test_repository`) is the `scope` item that we are importing into:

```bash
curl    -X POST \
        -H "X-User: mike" \
        -H "X-LogMessage: Testing EAD import" \
        -H "Content-type: text/xml" \
        --data @ead.xml \
        http://localhost:7474/ehri/import/ead?scope=nl-test_repository
```

Run this and again we get an import log like so:

    {"message":"Testing EAD import","updated":0,"created":1,"unchanged":0}
    
Running `GET` on the `/ehri/DocumentaryUnit` method lists the units in the system, giving us this
rather verbose output:

    [
        {
            "data": {
                "identifier": "test-doc"
            },
            "id": "nl-test_repository-test_doc",
            "meta": {
                "childCount": 0,
                "gid": 37,
                "watchedBy": 0
            },
            "relationships": {
                "describes": [
                    {
                        "data": {
                            "creationProcess": "IMPORT",
                            "extentAndMedium": "167 files",
                            "languageCode": "eng",
                            "languageOfMaterial": [
                                "eng",
                                "fra",
                                "deu",
                                "heb",
                                "ron",
                                "yid"
                            ],
                            "levelOfDescription": "collection",
                            "name": "Test EAD Item",
                            "scopeAndContent": "This is some test scope and content.",
                            "sourceFileId": "C00001#ENG"
                        },
                        "id": "nl-test_repository-test_doc.eng",
                        "meta": {
                            "gid": 38
                        },
                        "relationships": {
                            "hasDate": [
                                {
                                    "data": {
                                        "description": "1924-1-1 - 1947-12-31",
                                        "endDate": "1947-12-31",
                                        "startDate": "1924-01-01"
                                    },
                                    "id": "de1f5c77-6908-11e5-8ffa-0da85690eef2",
                                    "meta": {
                                        "gid": 42
                                    },
                                    "relationships": {},
                                    "type": "datePeriod"
                                },
                                {
                                    "data": {
                                        "description": "1943-1-1",
                                        "endDate": "1943-01-31",
                                        "startDate": "1943-01-01"
                                    },
                                    "id": "de1f5c79-6908-11e5-8ffa-0da85690eef2",
                                    "meta": {
                                        "gid": 43
                                    },
                                    "relationships": {},
                                    "type": "datePeriod"
                                }
                            ],
                            "hasUnknownProperty": [
                                {
                                    "data": {
                                        "ead_archdesc_did_langmaterial_language_": [
                                            "English",
                                            "French",
                                            "German",
                                            "Hebrew",
                                            "Romanian",
                                            "Yiddish"
                                        ],
                                        "ead_archdesc_did_repository_address_addressline_": [
                                            "Test Address 1",
                                            "Test Address 2"
                                        ],
                                        "ead_archdesc_did_repository_corpname_": "Test Corportate Body"
                                    },
                                    "id": "de1f5c75-6908-11e5-8ffa-0da85690eef2",
                                    "meta": {
                                        "gid": 41
                                    },
                                    "relationships": {},
                                    "type": "property"
                                }
                            ],
                            "relatesTo": [
                                {
                                    "data": {
                                        "name": "Test Name",
                                        "type": "creatorAccess"
                                    },
                                    "id": "de1f5c73-6908-11e5-8ffa-0da85690eef2",
                                    "meta": {
                                        "gid": 40
                                    },
                                    "relationships": {},
                                    "type": "relationship"
                                },
                                {
                                    "data": {
                                        "name": "Test Corporate Body",
                                        "type": "subjectAccess"
                                    },
                                    "id": "de1f5c71-6908-11e5-8ffa-0da85690eef2",
                                    "meta": {
                                        "gid": 39
                                    },
                                    "relationships": {},
                                    "type": "relationship"
                                }
                            ]
                        },
                        "type": "documentDescription"
                    }
                ],
                "heldBy": [
                    {
                        "data": {
                            "identifier": "test-repository"
                        },
                        "id": "nl-test_repository",
                        "meta": {
                            "childCount": 1,
                            "watchedBy": 0
                        },
                        "relationships": {
                            "describes": [
                                {
                                    "data": {
                                        "languageCode": "eng",
                                        "name": "Test Repository"
                                    },
                                    "id": "nl-test_repository.eng-test_repository_desc",
                                    "relationships": {},
                                    "type": "repositoryDescription"
                                }
                            ],
                            "hasCountry": [
                                {
                                    "data": {
                                        "identifier": "nl"
                                    },
                                    "id": "nl",
                                    "meta": {
                                        "childCount": 1
                                    },
                                    "relationships": {},
                                    "type": "country"
                                }
                            ]
                        },
                        "type": "repository"
                    }
                ],
                "lifecycleEvent": [
                    {
                        "data": {
                            "eventType": "ingest",
                            "timestamp": "2015-10-02T14:24:11.166+01:00"
                        },
                        "id": "de2db45d-6908-11e5-8ffa-0da85690eef2",
                        "meta": {
                            "childCount": 1
                        },
                        "relationships": {
                            "hasActioner": [
                                {
                                    "data": {
                                        "identifier": "mike",
                                        "name": "mike"
                                    },
                                    "id": "mike",
                                    "meta": {
                                        "followers": 0,
                                        "following": 0,
                                        "watching": 0
                                    },
                                    "relationships": {},
                                    "type": "userProfile"
                                }
                            ]
                        },
                        "type": "systemEvent"
                    }
                ]
            },
            "type": "DocumentaryUnit"
        }
    ]
    
Again, stuff to note:

 - the generated ID is `nl-test_repository-test_doc`, while the identifier property is `test-doc`
 - the repository is present in the `heldBy` relationship
 
## Additional import options

In these examples we POSTed a single XML file (with content-type `text/xml`) to the import methods. This is
convenient for single files, but often you want to import multiple XML files at once into the same country,
repository, or documentary unit (as child items.) In this case it is possible to create zip or tar archives
containing those files and POST the data as content type `application/octet-stream`. 

Another (less good) alternative when the web service server is local is to POST a file containing
a list (one entry per line) of local file paths with content-type `text/plain`