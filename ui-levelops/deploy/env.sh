#!/bin/sh

# Recreate config file
#rm -rf ./env-config.js || true
touch ./env-config.js
touch ./.env
set  | grep REACT_APP >> ./.env

# Add assignment
echo "window._env_ = {" >> ./env-config.js

# Read each line in .env file
# Each line represents key=value pairs
while read -r line || [[ -n "$line" ]];
do
  # Split env variables by character `=`
  if printf '%s\n' "$line" | grep -q -e '='; then
    varname=$(echo $line | cut -d'=' -f1)
    varvalue=$(echo $line | cut -d'=' -f2 | sed -e 's/'\''/ /g' | sed -e 's/ //g')
    #varname=$(printf '%s\n' "$line" | sed -e 's/=.*//')
    #varvalue=$(printf '%s\n' "$line" | sed -e 's/^[^=]*=//')
  fi
  echo $varname
  echo $varvalue

  # Read value of current variable if exists as Environment variable
  #value=$(printf '%s\n' "${!varname}")
  # Otherwise use value from .env file
  #[[ -z $value ]] && value=${varvalue}

  # Append configuration property to JS file
  echo "  $varname: \"$varvalue\"," >> ./env-config.js
done < .env

echo "}" >> ./env-config.js

