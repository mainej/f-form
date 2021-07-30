# Contributing

TODO: establish contributing guidelines here.

## Develop

* Run `bin/test` to run tests.
* Run `bin/coverage` for an updated code coverage report.

## Deploy

### Prepare

1. Run `bin/preview-tag` to learn new version number.
2. Proactively update CHANGELOG.md for new version numnber.
3. Commit
4. Tag commit `bin/tag-release`

### Release

Deploy to Clojars -- needs `CLOJARS_USERNAME` and `CLOJARS_PASSWORD` environment
variables:

    $ envdir ../../env/clojars bin/clojars-release

The library will be deployed to [clojars.org][clojars].

[clojars]: https://clojars.org/com.github.mainej/f-form
