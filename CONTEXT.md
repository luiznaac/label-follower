# label-follower domain glossary

The vocabulary label-follower uses for record labels, their releases and the playlist it builds from them. Glossary only — no implementation detail.

## Language

**Label**:
A record label whose releases the user follows on Spotify.
_Avoid_: publisher, imprint

**Track**:
A single recording released by a label, identified by its ISRC.
_Avoid_: song, release

**ISRC**:
International Standard Recording Code — the 12-character code that identifies a track across services and is the lookup key for every discovery flow.
_Avoid_: id, code

**Catalog**:
The recent releases a label has put out, as Spotify reports them at lookup time.
_Avoid_: discography, backlog

**Introspect**:
To read a label's catalog and compare it with the tracks label-follower already knows, so the difference is visible.
_Avoid_: scan, crawl

**New track**:
A track in a label's catalog that label-follower has not recorded yet.
_Avoid_: unreleased, discovery

**Consolidate**:
To introspect every followed label and build one Spotify playlist from all the new tracks found.
_Avoid_: sync, aggregate

**Followed label**:
A label the user has registered with label-follower; consolidation walks this set.
_Avoid_: saved label, subscription

**Spotify login**:
The Spotify account authorization label-follower uses to create playlists on the user's behalf.
_Avoid_: session, token
