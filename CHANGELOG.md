# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Generated by [`auto-changelog`](https://github.com/CookPete/auto-changelog).

## [--output](https://github.com/nguyenquy0710/Webhook-MockAPI/compare/1.0.4...--output)

### Commits

- chore(ci): update .version.txt for build 1.0.1 [`ff1d29b`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/ff1d29bd4b91986725a4ea22cf4d660c9419817a)

## [1.0.4](https://github.com/nguyenquy0710/Webhook-MockAPI/compare/1.0.3...1.0.4) - 2025-06-26

### Merged

- Develop [`#3`](https://github.com/nguyenquy0710/Webhook-MockAPI/pull/3)

### Commits

- Refactor HTML templates for improved readability and structure; enhance request-vars.html with additional JSON structure for headers and query parameters. [`0b2d733`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/0b2d73378fb49995dc2aee67d0b86c2cce19ceff)
- Add Swagger documentation for Logs API with user log retrieval and count endpoints [`076e3a2`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/076e3a2a5c32b230a65284a3b856796c19e42977)
- Add GitHub Actions for versioning and changelog updates on release [`4b0fe89`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/4b0fe8961bdcbe21199d73aa63ac69fba64b6619)
- Update CI/CD workflow to conditionally set versioning and push Docker image only on release events [`fed54b0`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/fed54b0afc64b2e33f9f7453796dff5a0621a27a)
- Comment out logging statements in applyTemplate method to reduce verbosity [`2bb0ca9`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/2bb0ca9b057430fde272f25a88133f691ccbc47e)
- Update ci-cd.yml [`146d40a`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/146d40a53a301a96ae4d651025aa2966bcaa898f)

## [1.0.3](https://github.com/nguyenquy0710/Webhook-MockAPI/compare/1.0.2...1.0.3) - 2025-06-26

### Merged

- Develop [`#2`](https://github.com/nguyenquy0710/Webhook-MockAPI/pull/2)

### Commits

- Add context variable documentation and enhance API configuration template [`7b83306`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/7b833061196df9b215d211c4179a4a7b6dbc9f0c)
- Refactor API configuration template to improve formatting and include context variable guide fragment [`f1ed922`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/f1ed922f6828475ea348042a97be1f11bedc1c19)
- Refactor context variable guide integration and replace obsolete fragment with updated version [`9e82164`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/9e82164b91494fa674413b08a3ac9826f352773d)
- Refactor ApiMockService to improve code readability and maintainability [`bafec0f`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/bafec0faa701d9b222c93a156e179bcc20b327dd)
- Add VS Code configuration files and custom dictionaries for spell checking [`29944ca`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/29944ca5e8490c6e5fff48641aae4b05166cc5b2)
- Refactor ApiMockController and ApiMockService for improved readability and maintainability; update processWebhook method to accept HttpServletRequest context. [`7b373de`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/7b373de222be35ffa01a7dfaefd605248deb3a32)
- Thêm file .editorconfig, cập nhật .gitignore để loại trừ các file cơ sở dữ liệu và log, cải tiến Dockerfile với thông tin người duy trì và cấu hình môi trường, cập nhật README.md với hướng dẫn mới và thông tin phiên bản, thêm script setup_java_home.sh để thiết lập JAVA_HOME tự động. [`b712acd`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/b712acd59e618cf03996300e6a338b2efda6e526)
- Refactor RequestUtils to extract request context and enhance template variable support; update ApiMockService to utilize new context extraction method. [`5502bad`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/5502badaf1c96b39fa0dc3a2990ce3957d4979c1)
- Enhance ApiMockService to support template variable replacement in response body and add RequestUtils for context building [`7fe732f`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/7fe732fc14b0ae042a43a1f2e0745766a8f9a320)
- Refactor template variable processing by moving logic to RequestUtils and improving logging [`2a35161`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/2a35161202d188f9893518b5afcf33d2e0e47ea0)
- Refactor API configuration template to include context variable guide and remove obsolete variable fragments [`3054fa7`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/3054fa72ad9612a0eddb9ad7b4508c1fc695cb4d)
- Enhance ApiMockService and RequestUtils to support HttpServletRequest context in webhook processing [`5539227`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/553922737decd4a3c4a4c50c2c79582c3dd62192)
- Fix context variable syntax in documentation to use {{variable}} format [`814b1d6`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/814b1d6f8ee8b7d28209ed0bd9c7ded5ca49797b)
- Make applyTemplate method public and enhance imports in RequestUtils [`cf64100`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/cf64100d926cc1cc0a3f81908211d58289a4fe53)
- Enhance template variable processing in ApiMockService with improved logging and string rendering [`e76fe46`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/e76fe462c05ea9fc03204c4ad36314a7359c66a8)
- Update custom dictionary and Dockerfile for improved configuration [`b1238fc`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/b1238fceb5c01bfd3e1df39198368c54e70a58a9)
- delete [`ccb2a51`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/ccb2a51b151e7cb2ceb9aaaf8616134d0edec067)
- Add VS Code extensions recommendations for improved development experience [`4e80f87`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/4e80f87ee75d780fcf061745920524e99effa311)
- Fix context variable syntax in request-vars.html to use correct {{variable}} format [`16b93f7`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/16b93f7be62f2de85f416e5934a18787471c9f02)
- Update context variable inclusion in API configuration edit template and clarify fragment file path [`53b9c80`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/53b9c8080ede58271b6285db62f25db4529a1144)
- Fix null handling in response body variable replacement in ApiMockService [`f4f33b5`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/f4f33b5ceb4412800d59fa4f9ed0fd092948df9b)
- Update CI/CD workflow to define branch and repository environment variables, and adjust versioning logic [`337d5cb`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/337d5cb8f01cdcaf508d58057db2901e83df6872)
- Add logging for processing template variables in ApiMockService [`e57c392`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/e57c3924fbfde222fd4b70ffbe5171bccfc257cb)
- Make mvnw and setup_java_home.sh executable [`111925c`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/111925cc8df24a079507cd6da80774ccd4005254)

## [1.0.2](https://github.com/nguyenquy0710/Webhook-MockAPI/compare/1.0.1...1.0.2) - 2025-06-05

### Commits

- Refactor Dockerfile to improve image size and clarity of comments [`233085b`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/233085bc36c638ba94ec430b9bf9d340bc047d05)
- Refactor Dockerfile to remove unnecessary font installations and clean up comments [`c3a151d`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/c3a151dd9b8d1ced31046a683a0524bd6d307b4c)
- update Dockerfile [`442675f`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/442675fb71ab8d0b9f22ef2fc822fd27db1def16)
- Fix syntax for LABEL instruction in Dockerfile [`735efef`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/735efef629d120c1b237720a743cf20d38e650be)
- update cicd [`cad6ce5`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/cad6ce577961484d331603f9cee2244aaf85760a)

## [1.0.1](https://github.com/nguyenquy0710/Webhook-MockAPI/compare/1.0.0...1.0.1) - 2025-06-05

### Commits

- Update README.md [`dddf6a2`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/dddf6a269090d3eaac9bc5c0373af19d01d4160d)
- Update ci-cd.yml [`609c45b`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/609c45b467e37c032ec0f5e73cfef5f80df0562d)

## 1.0.0 - 2025-06-05

### Commits

- First init project [`9c9e644`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/9c9e644f5801acc46fe3dc99479bfda6976c65e2)
- update ci-cd [`ea9fc32`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/ea9fc32de6d8b1d448fc021eee285e77e2f18d26)
- Create README.md [`e4cc689`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/e4cc68927ec877b67f89a0fbc51c3271c8217779)
- fix code [`35e3b76`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/35e3b76db6b8276542553967e9e96ecbcc1b2030)
- ag [`220b316`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/220b31645d457e4b1d0c6bdafa2b896a69160e04)
- up [`c11365a`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/c11365a357a4ad9299d9be39302d1e17fcdbdc19)
- change domain by config [`34c6be0`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/34c6be08a3c7092ed0c4dc7dc39a1de12a01bc80)
- Update config nginx deploy on server [`0db4a57`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/0db4a57623dc0ae46c151baa77ad381590f08068)
- fix gfooter [`2f2ed66`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/2f2ed66ccfbe7c2dc43becb2053e75949be1d77b)
- update ci-cf [`9038121`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/9038121390d9ccabea5e55abc61eb169da32b027)
- fix button register [`c960cfb`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/c960cfbd8defe7ab0d9cfd22d1adb4139af0a100)
- Create maven.yml [`9242bd4`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/9242bd4f8b18c16b76b2cc9eb8e1d5dcdaf9746d)
- update footer [`4fede75`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/4fede75a1d2d311e4b1b032e65f9021e42f054e7)
- add docker-compose [`a0cd777`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/a0cd7778a0d19908a04c60b7ee2bd618ea3d757e)
- fix button register [`ee7f5ac`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/ee7f5ac9029cfa0be832f43a00bc865f7decb300)
- Update CI/CD workflow to include versioning for Docker images [`e915f77`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/e915f77383b62a038b186f95f12e52348d4cc74f)
- update port and config timezone default [`7c70feb`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/7c70feb633755cd14b94ce0759e25e94275773a3)
- add FUNDING [`11eff8d`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/11eff8d62883d5018aede8f038813db4c39437b7)
- asss [`42c035d`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/42c035dc4b27f54f4582a5e20b38019c78ea6b22)
- change me [`04461f1`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/04461f12123e17e2598c38fbe3c1a012ba79558b)
- b [`48b4195`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/48b4195547736a31ae9517d3b7b54509310c0927)
- a [`d80d098`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/d80d098f816ae04d2a6e2dc185655d093cab599a)
- update docker-compose [`0c3adf9`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/0c3adf98c61cbb97b83a3a38df923197414096c1)
- fix remote ip [`6a0f2c1`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/6a0f2c13c7c2e00f6c05c4e24a71e03c4a1fbd22)
- Move downloaded JAR to correct location [`b6a1897`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/b6a1897871783fdbafd911199fa19021879ce601)
- Move JAR to root [`2b9c4e7`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/2b9c4e726c4d64a5f466556baf9921b436316f5e)
- update Docker image [`f0baa57`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/f0baa57a2145cb665a9760e040412e2c0db9eab8)
- staging [`1654821`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/16548219524af767b87258c98823d97140565a72)
- up [`c223f6f`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/c223f6f70d5647d0a73f33d9ee796f736cd479ba)
- as [`9e28127`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/9e2812783fcf523b328a823755c591f74055fb7d)
- staging s [`eec4993`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/eec49933764643e4191fc136650874a530062467)
- clear data temp [`80fd263`](https://github.com/nguyenquy0710/Webhook-MockAPI/commit/80fd263401537dc716d860629668085c2fc558a3)
