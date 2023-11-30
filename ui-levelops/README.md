# Propelo UI

Propelo UI repo

This is the main repo for everything front-end Propelo

### Setting up the dev environment
1. First thing to do is to clone the repo: `git clone https://github.com/levelops/ui-levelops.git`
2. We'll need to download some private packages from Harness Org so we'll need to new github token for it 
    1. Go here https://github.com/settings/tokens
    2. Create a token with 'repo' and 'read:packages' scopes
3. Create a new file from the root `.npmrc` and add in the new token to the end of it 
```
@harness:registry=https://npm.pkg.github.com
//npm.pkg.github.com/:_authToken= ___your new token here ___
```
4. run clean-up first (just in case) `rm -rf node_modules && rm package-lock-json`
5. run `npm install --force`, we'll need to resolve the security compliance issues later 
6. You'll need to create `.env` file, copy the values from `ui-levelops/public/env-config.js`. Make sure you sure `=` instead of `:`
```
# NODE_ENV=development
SKIP_PREFLIGHT_CHECK=true
REACT_APP_API_URL=https://testapi1.propelo.ai
REACT_APP_API_VERSION=v1
REACT_APP_GITHUB_CLIENT_ID=someString
REACT_APP_JIRA_CLIENT_ID=someString
REACT_APP_SLACK_CLIENT_ID=someString
REACT_APP_UI_URL=http://localhost:3000
REACT_APP_MODE=local
REACT_APP_DEBUG=false
REACT_APP_TINYMCE_API_KEY=someString
REACT_APP_BUGSNAG_NOTIFIER_API_KEY=55568e8ffc0f7c66d986d1e6857ab86a
REACT_APP_ALL_ACCESS_USERS=customersuccess@levelops.io,customersuccess@propelo.ai 
```
7. Now, you should be able to run `npm run new_dev`  
