# OnBoarding Backend

This is the back-end to the sibling [onboarding](https://github.com/aaronp/onboarding) project.

## Development

For active development, this backend can be run within the front-end project, providing an immediate feedback DevX experience.

Run:

```shell
sbt
project appJS 
~fastLinkJS
```

which produces `./js/target/scala-3.4.1/app-fastopt/main.mjs`

That output file is then included in the onboarding package, invoking service calls directly

## Production

For production builds, the backend and frontend services are deployed onto kubernetes.

The injected interfaces used in production are then implemented by REST calls as opposed to direct invocation.

See [here for more](./docs/building.md) for more details.