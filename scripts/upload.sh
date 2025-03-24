set -e
scala-cli --power package --main-class Main --docker . --docker-image-repository pompano-golf-reservation
docker save -o ./pompano-golf-reservation.tar pompano-golf-reservation
scp ./docker-compose.yml ./.env ./pompano-golf-reservation.tar server1:/pompano-golf-reservation
ssh server1 "docker load -i /pompano-golf-reservation/pompano-golf-reservation.tar && docker compose -f /pompano-golf-reservation/docker-compose.yml up -d"

