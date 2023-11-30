#! /bin/bash

asia1Api=c18e9f2bd45885624e9f702b1a3bc623
eu1Api=f31bf570ed9688061123dbca6d6eb21f
prodApi=378366609df12f0827c6c6869ceb3667
stagingApi=7e412cf967798cecbc9c49460f8ac611

while getopts e:v: flag
do
    case "${flag}" in
        e) env=${OPTARG};;
        v) version=${OPTARG};;
    esac
done

# this will convert env into an array
envArray=(${env//,/ })

echo "Uploading with current: ${version}"

if [ ${#envArray[@]} -eq 0 ]; then
    echo "No environments are defined for uploading; please use -e to define it"
else
    if [ ${#envArray[@]} -eq 1 -a ${envArray[0]} = "allProd" ]; then
        echo 'Uploading sourcemaps to all environments'
        npm run build        
        # asia1 
        npx bugsnag-source-maps upload-browser --api-key ${asia1Api} --app-version 0.1.${version} --base-url https://asia1.app.propelo.ai/static/js/ --directory ./../build/static/js

        # eu1
        npx bugsnag-source-maps upload-browser --api-key ${eu1Api} --app-version 0.1.${version} --base-url https://eu1.app.propelo.ai/static/js/ --directory ./../build/static/js

        # us prod, both propelo and levelops domains
        npx bugsnag-source-maps upload-browser --api-key ${prodApi} --app-version 0.1.${version} --base-url https://app.propelo.ai/static/js/ --directory ./../build/static/js
        npx bugsnag-source-maps upload-browser --api-key ${prodApi} --app-version 0.1.${version} --base-url https://app.levelops.io/static/js/ --directory ./../build/static/js

        # staging as well
        npx bugsnag-source-maps upload-browser --api-key ${stagingApi} --app-version 0.1.${version} --base-url https://staging.app.propelo.ai/static/js/ --directory ./../build/static/js
    else 
        echo 'theres more than one defined'

        # we should do a loop here to check which env are used for uploading
    fi
fi