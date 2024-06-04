#!/usr/env/bin bash

generate() {
	docker pull openapitools/openapi-generator-cli:latest

    # Iterate over all subdirectories under schemas
    for dir in "schemas"/*/; do
        # Get the base name of the directory
        dirname=$(basename "$dir")

        # Print the directory being processed

        echo ""
        echo "   +-------------------------------------------------"
        echo "   + Processing $dirname"
        echo "   +-------------------------------------------------"
        echo ""

        # Run the Docker command with the subdirectory mounted
        docker run --rm -v "${PWD}:/local" openapitools/openapi-generator-cli:latest generate \
          -i /local/$dir/service.yaml \
          -g scala-cask \
          -c /local/$dir/openapi-config.yaml \
          -o /local/target/$dir; \
    done
}

buildModels() {

    # Iterate over all subdirectories under schemas
    for dir in "schemas"/*/; do
        # Get the base name of the directory
        dirname=$(basename "$dir")

        # Print the directory being processed

        echo ""
        echo "   +-------------------------------------------------"
        echo "   + Publishing $dirname"
        echo "   +-------------------------------------------------"
        echo ""

        echo "Publishing $dirname"
		    pushd "target/schemas/$dirname"
        sbt publishLocal
        popd
    done
}


genModels() {
	generate && buildModels
}
