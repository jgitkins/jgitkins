# Bounded Context

이 폴더는 bounded context 단위의 문서 허브다.

## Documents

- [Overview](./overview.md)
  - 도메인 용어와 경계 초안
- [Aggregates](./aggregates.md)
  - aggregate 후보와 전체 도메인 맵
- [Repository Context](./repository/README.md)
  - 저장소 경계, 초기화, 멤버십
- [Change & Review Context](./change-review/README.md)
  - Pull Request, target drift, mergeability
- [Execution Context](./execution/README.md)
  - Job, JobHistory, Runner
  - [Jobs Deep Dive](./execution/jobs/README.md)
- [Identity & Access Context](./identity-access/README.md)
  - User, UserIdentity, UserCredential
- [Collaboration Context](./collaboration/README.md)
  - Organization, Organization Member
- [Shared / Cross-Cutting Topics](./shared/README.md)
  - namespace, access resolution, mergeability policy
- [Policies](./policies/README.md)
  - exception handling, repository permission
