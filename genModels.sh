#!/usr/env/bin bash

genModels() {
	docker pull openapitools/openapi-generator-cli:latest

    # Iterate over all subdirectories under schemas
    for dir in "schemas"/*/; do
        # Get the base name of the directory
        dirname=$(basename "$dir")

        # Print the directory being processed
        echo "Processing directory $dir (name: $dirname)"

        # Run the Docker command with the subdirectory mounted
		docker run --rm -v "${PWD}:/local" openapitools/openapi-generator-cli:latest generate \
			-i /local/$dir/service.yaml \
			-g scala-cask \
			-c /local/$dir/openapi-config.yaml \
			-o /local/target/$dir; \

        echo "Publishing $dirname"
		cd "target/schemas/$dirname" && sbt publishLocal
    done
}