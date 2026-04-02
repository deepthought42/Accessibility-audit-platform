## [1.2.6](https://github.com/deepthought42/PageBuilder/compare/v1.2.5...v1.2.6) (2025-07-19)


### Bug Fixes

* upgrade to 0.3.16 ([73ba16d](https://github.com/deepthought42/PageBuilder/commit/73ba16d87397d9dab8a5e0683495e8d2ff8d9620))

## [1.2.5](https://github.com/deepthought42/PageBuilder/compare/v1.2.4...v1.2.5) (2025-07-19)


### Bug Fixes

* add selenium config ([8475094](https://github.com/deepthought42/PageBuilder/commit/8475094d2650735e715819d60222f88524234b84))
* core upgrade to 0.3.15 and refactor remaining classes and move some to core library ([8d37486](https://github.com/deepthought42/PageBuilder/commit/8d374869b7b4a644d07e1c78b1e20c4bfec3f65b))
* download core ([eaf7b0b](https://github.com/deepthought42/PageBuilder/commit/eaf7b0baac6ce05f9f76e9a8e7fcea6c192aab8d))
* prevent pipeline running for push to master ([005423f](https://github.com/deepthought42/PageBuilder/commit/005423fd4f10b1ad22de40d9bec40b66244568f9))
* type in core jar file name for CI step ([ed389a7](https://github.com/deepthought42/PageBuilder/commit/ed389a780ca17821432fde3e3e7782ccbf25b7b6))
* typo in value selenium urls ([743f21a](https://github.com/deepthought42/PageBuilder/commit/743f21ab445e845a3be87ad2e16f9291635d6116))
* update core and config selenium ([0fe69a8](https://github.com/deepthought42/PageBuilder/commit/0fe69a889d92d6f2b335d8ea1df374b70cf4bb57))

## [1.2.4](https://github.com/deepthought42/PageBuilder/compare/v1.2.3...v1.2.4) (2025-05-13)


### Bug Fixes

* fixed double pipeline trigger issue ([34f5a8a](https://github.com/deepthought42/PageBuilder/commit/34f5a8a462b8ad6abac95a461d5d7508a07b7cfc))

## [1.2.3](https://github.com/deepthought42/PageBuilder/compare/v1.2.2...v1.2.3) (2025-04-24)


### Bug Fixes

* removed gcp credential location ([f528091](https://github.com/deepthought42/PageBuilder/commit/f5280918b4873eb199efc5335178303efe6bc43d))

## [1.2.2](https://github.com/deepthought42/PageBuilder/compare/v1.2.1...v1.2.2) (2025-03-27)


### Bug Fixes

* updated staging dockerfile ([6a714ad](https://github.com/deepthought42/PageBuilder/commit/6a714ad9b77adb6c08ad7c48763db6faefc5cb21))

## [1.2.1](https://github.com/deepthought42/PageBuilder/compare/v1.2.0...v1.2.1) (2025-03-26)


### Bug Fixes

* mvn install skips tests ([44cee5e](https://github.com/deepthought42/PageBuilder/commit/44cee5eb3bbef391fa90decf293cd96443d2fb74))

# [1.2.0](https://github.com/deepthought42/PageBuilder/compare/v1.1.0...v1.2.0) (2025-03-26)


### Bug Fixes

* bump-version relies on test step ([b5b8061](https://github.com/deepthought42/PageBuilder/commit/b5b8061326e980af019e8e3d7ab414888c5b073e))
* fixed github actions yml errors ([121ea3e](https://github.com/deepthought42/PageBuilder/commit/121ea3e889cb2eda1e53f1eb4f1f3b0e1ac8c08c))
* moved maven test to beginning of test step ([a0ba503](https://github.com/deepthought42/PageBuilder/commit/a0ba503d087b3242d982393c1ad26f318ae74f46))


### Features

* bump version put in separate step ([213c676](https://github.com/deepthought42/PageBuilder/commit/213c676c202809bf4bc6d94960ad8381ad03045f))

# [1.1.0](https://github.com/deepthought42/PageBuilder/compare/v1.0.5...v1.1.0) (2025-03-26)


### Features

* added separate job for github release ([0f9494a](https://github.com/deepthought42/PageBuilder/commit/0f9494a97c3653787da4d44b3a4da9f5758b5b98))

## [1.0.5](https://github.com/deepthought42/PageBuilder/compare/v1.0.4...v1.0.5) (2025-03-26)


### Bug Fixes

* added code checkout to release step ([28ebcd4](https://github.com/deepthought42/PageBuilder/commit/28ebcd4ee0239820184a8a03a67007525e338f49))

## [1.0.4](https://github.com/deepthought42/PageBuilder/compare/v1.0.3...v1.0.4) (2025-03-26)


### Bug Fixes

* made version available across steps ([350562e](https://github.com/deepthought42/PageBuilder/commit/350562e23870c4a77664e59d49335cdf3615a3e2))

## [1.0.3](https://github.com/deepthought42/PageBuilder/compare/v1.0.2...v1.0.3) (2025-03-26)


### Bug Fixes

* changed build to test ([477bdc7](https://github.com/deepthought42/PageBuilder/commit/477bdc771d72bf836fdc6333ac2d7560a5ae4d6c))
* moved docker build into build and release step ([a4f73a8](https://github.com/deepthought42/PageBuilder/commit/a4f73a84f06d843786b8338c2bdd29b31dd3a553))

## [1.0.2](https://github.com/deepthought42/PageBuilder/compare/v1.0.1...v1.0.2) (2025-03-26)


### Bug Fixes

* corrected jar file name for java run command ([66e6d9f](https://github.com/deepthought42/PageBuilder/commit/66e6d9f1326de81d90f210e1ce8790dc5ff4938c))

## [1.0.1](https://github.com/deepthought42/PageBuilder/compare/v1.0.0...v1.0.1) (2025-03-25)


### Bug Fixes

* release CI step relies on build ([c1a1e64](https://github.com/deepthought42/PageBuilder/commit/c1a1e64bc52695438d962664a1edba12d64c2a65))

# 1.0.0 (2025-03-25)


### Bug Fixes

* performed npm install ([ded3a98](https://github.com/deepthought42/PageBuilder/commit/ded3a983452730f958266fadfd0a98f54d3c6f57))
* replaced incorrect project names ([5a05c8c](https://github.com/deepthought42/PageBuilder/commit/5a05c8c07078e029f0782977f27857d091d24dac))


### Features

* added semantic release ([7a8eedc](https://github.com/deepthought42/PageBuilder/commit/7a8eedc226cfae55477dbebca53db44924ac9605))
* script to bump project version in pom ([ce68e3b](https://github.com/deepthought42/PageBuilder/commit/ce68e3beeadab48efb67aaae2f19736e84d256b6))
