## usage: ./run-dev-satellite.sh ~/Downloads/satellite.yml
echo "• Stopping pre-existing satellite..."
docker stop sat-dev
echo "• Running new satellite with config file '$1':"
docker run --name sat-dev --rm -d -v $1:/levelops/config.yml sat-dev && docker logs sat-dev -f
