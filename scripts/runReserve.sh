export $(cat .env.development | xargs)
scala-cli run . --main-class RunReserve