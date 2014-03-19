Database Migration Changelog
----------------------------

- ???? - The backend added a 'promote' permission to override default visibility for user-created content. The
`scripts/ruby/create_promote_perm.rb` script added this new permission's node to the DB.
- 03/01/2014 - The backend code was altered to make the (3-letter) language code the per-item
description discriminator (along with the `descriptionIdentifier` field) for descriptions, meaning their IDs could be generated deterministically instead of randomly as before. The `scripts/ruby/migrate_description_ids.rb` updated the existing descriptions accordingly.
- 03/01/2014 - The script 'scripts/ruby/migrate_639-1_639-2_langs.rb` updated all language code fields from their
2-letter to 3-letter representations.
- 03/12/2013 - The `scripts/ruby/set_system_event_child_count.rb` and `scripts/ruby/set_country_repo_child_count.rb`
was used to cache the number of 'child' relations a node has on its _CHILD_COUNT meta property. An update to the code
 henceforth maintains this count on hierarchical relationships.
- 03/12/2013 - Debugging the global and per-item event chains proved difficult because there were several 'link'
used that had no properties and only served to connect two 'first-class' nodes via an indirection. The script
`scripts/ruby/stamp_link_nodes.rb` set debugging properties `_debugType` and `_linkType` on these nodes.
- 20/10/2013 - The initial import of EAD data from ICA-Atom had missed the archivist's note field. The
`scripts/ruby/fix_icaatom_import.rb` script corrected this by setting this data on the graph nodes from a custom CSV
dump of the ICA-Atom MySQL DB.
- 20/10/2013 - Fixed an issue (on the frontend) where saving/updating a repository would erase any maintenance event
nodes it was connected to. The script `scripts/ruby/fix_maintenance_events.rb` restored that maintenance events that
were erroneously deleted.
- 10/10/2013 - Fixed an issue where some repositories were imported without the correct (country) permission scope.
This led to their documents having incorrect IDs. The script `scripts/ruby/reset_doc_ids.rb` was used to set the
proper permission scope and recalculate IDs for all items in the hierarchy (country -> repo -> doc)