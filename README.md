# kind Logic-First Project

This project was generated from the [kindservices/logic-first.g8](https://github.com/kindservices/logic-first.g8) template.

See [Kind Services](https://www.kindservices.co.uk/) for more on a Logic-First approach to executable architecture.

# Building

The accompanying [Makefile](./Makefile) has:

### generateModels - create REST stubs
`make generateModels` will iterate through all the (./schemas)[./schemas] subdirectories, expecing a `service.yml` and `openapi-config.yml` in each.

It will generate and the package up the scala stubs for each so that they can be used within this project in both the JVM and JS projects

To add a new service/generated data model, just copy and modify the existing (example)[./schemas/example]

### runUI - quickly see your changes in the browser
For local development, the typical workflow is:

Continually compile the project:
```sh
sbt "project appjs" ~fastLinkJS
```

And then serve it up using vite
`make runUI` 


### packageUI - package up the javascript form for your project
`make packageUI` will package up a zip file which you can then extract and serve by using either:

NPM:
```sh
npm install -g serve
serve -s dist
```

or Python3:
```sh
python3 -m http.server
```


## On the JVM

This project is built using [sbt](https://www.scala-sbt.org/):

```sh
sbt ~compile
```

Or, for a zero-install (but likely much slower) docker build:

```sh
docker run -it --rm -v ${PWD}:/app -w /app hseeberger/scala-sbt:8u222_1.3.5_2.13.1 sbt compile
```