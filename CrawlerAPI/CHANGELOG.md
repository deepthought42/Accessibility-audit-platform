## [1.2.1](https://github.com/deepthought42/CrawlerApi/compare/v1.2.0...v1.2.1) (2025-05-04)


### Bug Fixes

* removed secret from properties file ([ed2eedd](https://github.com/deepthought42/CrawlerApi/commit/ed2eedd0dd304032a4bdb82210ccb2553be39b3a))

# [1.2.0](https://github.com/deepthought42/CrawlerApi/compare/v1.1.8...v1.2.0) (2025-04-28)


### Features

* removed auth0 for portability and ease of deployment ([900a08e](https://github.com/deepthought42/CrawlerApi/commit/900a08ede0b6d8635f70f2dbaec526bb677449df))

## [1.1.8](https://github.com/deepthought42/CrawlerApi/compare/v1.1.7...v1.1.8) (2025-04-28)


### Bug Fixes

* removed oauth jwt resource ([15800da](https://github.com/deepthought42/CrawlerApi/commit/15800da8205b4d2448dfe0267cae7897df690af9))

## [1.1.7](https://github.com/deepthought42/CrawlerApi/compare/v1.1.6...v1.1.7) (2025-04-26)


### Bug Fixes

* added auth0 properties ([d6b66c6](https://github.com/deepthought42/CrawlerApi/commit/d6b66c6f1a3cbd428aac37f96d348ad16e6c03ba))

## [1.1.6](https://github.com/deepthought42/CrawlerApi/compare/v1.1.5...v1.1.6) (2025-04-26)


### Bug Fixes

* added resource folder to pom file ([189f1ce](https://github.com/deepthought42/CrawlerApi/commit/189f1ce69fbff8d7e0643ce6a1e0a0568c4da2a9))
* url check update ([5443820](https://github.com/deepthought42/CrawlerApi/commit/5443820fda75f84aebb7a95033121afaff6e6fdf))

## [1.1.5](https://github.com/deepthought42/CrawlerApi/compare/v1.1.4...v1.1.5) (2025-04-26)


### Bug Fixes

* pubsub topic config cleanup ([0bfe631](https://github.com/deepthought42/CrawlerApi/commit/0bfe6318c643efd14a92db3f9cbd8cb3e5caf5b4))

## [1.1.4](https://github.com/deepthought42/CrawlerApi/compare/v1.1.3...v1.1.4) (2025-04-26)


### Bug Fixes

* fixed gitignore for resource folder ([0a35661](https://github.com/deepthought42/CrawlerApi/commit/0a35661f9b42097878f0f441a77a3b2741958194))

## [1.1.3](https://github.com/deepthought42/CrawlerApi/compare/v1.1.2...v1.1.3) (2025-04-26)


### Bug Fixes

* remove secrets from application.properties and allow it to be tracked ([1eb8497](https://github.com/deepthought42/CrawlerApi/commit/1eb8497d635c0dfc6e3804883d633a511fd3b76c))
* removed assets that no longer exist ([be96465](https://github.com/deepthought42/CrawlerApi/commit/be9646544847ef6e76a4f67e8c47c1d222582eae))

## [1.1.2](https://github.com/deepthought42/CrawlerApi/compare/v1.1.1...v1.1.2) (2025-03-26)


### Bug Fixes

* mvn install skips tests ([06eed96](https://github.com/deepthought42/CrawlerApi/commit/06eed967f5f0dc76321c179fc243454720b99cc5))

## [1.1.1](https://github.com/deepthought42/CrawlerApi/compare/v1.1.0...v1.1.1) (2025-03-26)


### Bug Fixes

* bump version steps separated from test steps ([e52caa0](https://github.com/deepthought42/CrawlerApi/commit/e52caa0ff3f96f2ac84fab6850298edef5ee870f))

# [1.1.0](https://github.com/deepthought42/CrawlerApi/compare/v1.0.9...v1.1.0) (2025-03-26)


### Features

* added step for github release. moved mvn test to beginning of test step ([1aa7d66](https://github.com/deepthought42/CrawlerApi/commit/1aa7d66d5276c84918d1bf0b915a4f3e32884597))

## [1.0.9](https://github.com/deepthought42/CrawlerApi/compare/v1.0.8...v1.0.9) (2025-03-26)


### Bug Fixes

* made version available across jobs ([05e693e](https://github.com/deepthought42/CrawlerApi/commit/05e693e83700a37c1f3791f5aa546756dea55697))

## [1.0.8](https://github.com/deepthought42/CrawlerApi/compare/v1.0.7...v1.0.8) (2025-03-26)


### Bug Fixes

* package name to lowercase ([7ca76c8](https://github.com/deepthought42/CrawlerApi/commit/7ca76c835042b8f190f3cc8f56995d61c0401109))

## [1.0.7](https://github.com/deepthought42/CrawlerApi/compare/v1.0.6...v1.0.7) (2025-03-26)


### Bug Fixes

* moved docker image build to build and release step. renamed build to test ([169a1d3](https://github.com/deepthought42/CrawlerApi/commit/169a1d3d06394b99d31394cd701a57e4fae4efde))

## [1.0.6](https://github.com/deepthought42/CrawlerApi/compare/v1.0.5...v1.0.6) (2025-03-26)


### Bug Fixes

* release step relies on build step ([7212de4](https://github.com/deepthought42/CrawlerApi/commit/7212de458211a6ab6c2cfe6b1f3840c61c4a85cc))

## [1.0.5](https://github.com/deepthought42/CrawlerApi/compare/v1.0.4...v1.0.5) (2025-03-24)


### Bug Fixes

* fixed docker image name ([0f9fb3b](https://github.com/deepthought42/CrawlerApi/commit/0f9fb3bc028da58df287a99b1173658bb0dd1dff))
* set workdir to app for build in docker ([c922354](https://github.com/deepthought42/CrawlerApi/commit/c92235410ff3ff1da5833a940d4a33c313100c7c))

## [1.0.4](https://github.com/deepthought42/CrawlerApi/compare/v1.0.3...v1.0.4) (2025-03-22)


### Bug Fixes

* removed target folder from dockerfile copy ([1beecb4](https://github.com/deepthought42/CrawlerApi/commit/1beecb437cb80a59e43ba6810835b76378f41706))

## [1.0.3](https://github.com/deepthought42/CrawlerApi/compare/v1.0.2...v1.0.3) (2025-03-22)


### Bug Fixes

* changed order of steps in build process ([7d92f8a](https://github.com/deepthought42/CrawlerApi/commit/7d92f8ace0f549b65ade5e7faf002698889266f5))

## [1.0.2](https://github.com/deepthought42/CrawlerApi/compare/v1.0.1...v1.0.2) (2025-03-21)


### Bug Fixes

* dynamic version for jar file in Dockerfile ([8c86df0](https://github.com/deepthought42/CrawlerApi/commit/8c86df0608cd20da9f1cb21ca07662e74e8515cb))

## [1.0.1](https://github.com/deepthought42/CrawlerApi/compare/v1.0.0...v1.0.1) (2025-03-21)


### Bug Fixes

* fixed dockerfile ([d0bb374](https://github.com/deepthought42/CrawlerApi/commit/d0bb3741ed400a27459a0a08aca75422534657cc))

# 1.0.0 (2025-03-21)


### Bug Fixes

* changes releaserc branch name ([70b0c5a](https://github.com/deepthought42/CrawlerApi/commit/70b0c5a3f2f622d8abe6f4bc3d8ceaacc5f53e6d))
* cleaned up tests and contribution documentation ([f7a1ea2](https://github.com/deepthought42/CrawlerApi/commit/f7a1ea21f1120e42b39926ef81707d9c76c6b178))
* removed commit analyzer preset ([6086e99](https://github.com/deepthought42/CrawlerApi/commit/6086e99838dd44712f49dc68f75f769e0c967ad6))
* set pipeline to run when push and PR to master branch ([ccaae2d](https://github.com/deepthought42/CrawlerApi/commit/ccaae2dcc0bf29daef313ac43dda1c052b36e53a))


### Features

* contribution guide and version update in pipeline ([6d6d907](https://github.com/deepthought42/CrawlerApi/commit/6d6d90770a15c9bfecb6eadb81a03bfe6012fb43))


### Reverts

* Revert "Pagespeed insights integration ([#422](https://github.com/deepthought42/CrawlerApi/issues/422))" ([#423](https://github.com/deepthought42/CrawlerApi/issues/423)) ([24b079c](https://github.com/deepthought42/CrawlerApi/commit/24b079c1bf35bcd0cebe6838fe792fc93d68d330))
