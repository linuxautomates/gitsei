#!/bin/sh

# Redirect output to stderr.
exec 1>&2
# enable user input
exec < /dev/tty

consoleregexp='^\+.*console.log\.'
# CHECK
if test $(git diff --cached | grep $consoleregexp | wc -l) != 0
then
  read -p "❗ There are some occurrences of console statements at your modification. Are you sure want to continue? (y/n) " yn
  echo $yn | grep ^[Yy]$
  if [ $? -eq 0 ]
  then
    exit 0; #THE USER WANTS TO CONTINUE
  else
    exit 1; # THE USER DOES NOT WANT TO CONTINUE SO ROLLBACK
  fi
else
  echo "No console statements added."
fi


# https://gist.github.com/guilherme/9604324
