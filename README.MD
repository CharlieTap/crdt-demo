# Contacts δ-CRDT Demo

This demo project exhibits client nodes which propagate delta based CRDT state changes.

The server is merely a relay for the clients, it aggregates an event source of all delta changes propagated from each client.

Each client features the following:

- A SQLite database which functions as a [GSet](https://en.wikipedia.org/wiki/Conflict-free_replicated_data_type#G-Set_(Grow-only_Set)) of LWW Maps
- A [HybridLogicalClock](https://cse.buffalo.edu/tech-reports/2014-04.pdf) to help maintain the causal ordering of events between its own and those of other clients
- A delta generator process which simulates the creation, mutation and deletion of contacts 

# Architecture

```mermaid
graph TD;
 Server--Delta-->ClientA;
 Server--Delta-->ClientB;
 ClientA--Delta-->Server;
 ClientB--Delta-->Server;
```

# On disk data representation

On each client a SQLite database is maintained.

```sqlite
CREATE TABLE contact (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    name_timestamp TEXT NOT NULL,
    phone TEXT NOT NULL,
    phone_timestamp TEXT NOT NULL,
    email TEXT NOT NULL,
    email_timestamp TEXT NOT NULL,
    tombstone TEXT NOT NULL,
    tombstone_timestamp TEXT NOT NULL
);
```

You can imagine each row to be an entry in GSet, meaning it can never be deleted.   
Deletions from the table are managed by the tombstone field, meaning we really only soft delete data.
The set determines the identity of each element by an id, in this case the contacts id.  

Each element in the GSet is in fact an LWW Map.

To illustrate this, imagine the following Contacts data structure:

```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "name": "Bob",
  "phone": "555777889",
  "email": "boo@foo.com",
  "tombstone": "0"
}
```

This structure is in fact a map, for example **name**, **phone** are keys and Bob, 555777889 are values   
In order to create a Last Write Wins map we need a timestamp against each value. Note that we have omitted id here,
only mutable values need to be synchronised.


```json
{
  "name": { "value": "Bob", "timestamp": "123456789"},
  "phone": { "value": "555777889", "timestamp": "123456789"},
  "email": { "value": "boo@foo.com", "timestamp": "123456789"},
  "tombstone": { "value": "0", "timestamp": "123456789"}
}
```

To avoid normalisation and the associated performance dip we chose to flatten the structure 
rather than normalise the value and the timestamp into a sub table. Et Voilà

```json
{
  "name": "Bob",
  "name_timestamp": "123456789",
  "phone": "555777889",
  "phone_timestamp": "123456789",
  "email": "boo@foo.com",
  "email_timestamp": "123456789",
  "tombstone": "0",
  "tombstone_timestamp": "123456789"
}
```

# Synchronisation δ-CRDT

Delta based CRDTs synchronise themselves using messages which encode the 
delta effect of mutations.

For example if we changed Bob's name to Jim, the message would have the following 
structure.

```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "timestamp": "123456789",
  "column": "name",
  "value": "Jim"
}
```



