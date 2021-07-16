# Contributing

TODO: establish contributing guidelines here.

## Develop

* Run `bin/test` to run tests.
* Run `bin/coverage` for an updated code coverage report.

## Deploy

### Prepare

1. Decide whether this will be a patch, minor, or major release.
    * Run `bin/preview-tag --patch` (or --minor, --major) to learn new version number.
2. Proactively update CHANGELOG.md for new version numnber.
3. Commit

### Release

Deploy to Clojars -- needs `CLOJARS_USERNAME` and `CLOJARS_PASSWORD` environment
variables:

    $ envdir ../../env/clojars bin/clojars-release --patch # patch, minor, or major

The library will be deployed to [clojars.org][clojars].

Push to github, with tags:

    $ git push --follow-tags

[clojars]: https://clojars.org/com.github.mainej/f-form
