# epiccastle.io

Source code for website for https://epiccastle.io

## Dependency

The site is built using [bootleg](https://github.com/retrogradeorbit/bootleg). Install bootleg using the instructions on that page.

## Building

    make build

## Testing

1. Run the test server with:

        make runserver

2. Point your browser at http://localhost:8000

## Deploy

    make deploy

## Spire pages

To generate the spire site pages you will need to have spire project
checked out as a sibling directory to the root of this
directory. ie. going `cd ../spire` from this directory takes you to
the spire root.
