# Contributing

TODO: establish contributing guidelines here.

## Develop

* Run `bin/test` to run tests.
* Run `bin/coverage` for an updated code coverage report.
* Generate documentation
    * After cloning this project, run `git fetch origin gh-pages && git worktree add gh-pages gh-pages`.
    * Run `bin/doc` to generate documentation, accessible at `gh-pages/index.html`.
    * Run `bin/doc-release` to publish documentation to Github Pages.

## Deploy

### Prepare

1. Decide whether this will be a patch, minor, or major release.
2. Proactively update CHANGELOG.md for new version.
3. Commit

### Release

Deploy to Clojars -- needs `CLOJARS_USERNAME` and `CLOJARS_PASSWORD` environment
variables:

    $ envdir ../../env/clojars clojure -M:release --patch # patch, minor, or major

The library will be deployed to [clojars.org][clojars].

Push to github, with tags:

    $ git push --follow-tags

[clojars]: https://clojars.org/com.github.mainej/f-form
