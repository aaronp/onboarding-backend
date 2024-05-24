# This starts vite and sbt in parallel so you can run the JS code in the browser
dev:
	npm install
	npm run dev

packageUI:
	npm run build && npm run package

generateModels:
	source ./genModels.sh && genModels