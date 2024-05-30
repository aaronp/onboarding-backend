IMAGE = porpoiseltd/kind-docstore-db
LABEL = latest

run:
	sbt run
assembly:
	sbt assembly
build: assembly
	docker build . -t $(IMAGE)
login:
	docker login
useBuildX:
	docker buildx create --use
# assumes docker login
push: login useBuildX assembly
	docker buildx build --platform linux/amd64,linux/arm64 -t $(IMAGE):$(LABEL) --push .