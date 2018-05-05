create extension "uuid-ossp";

CREATE TABLE persons(
  person_id UUID NOT NULL PRIMARY KEY,
  first_name     varchar(40) NOT NULL,
  last_name      varchar(40) NOT NULL
);

