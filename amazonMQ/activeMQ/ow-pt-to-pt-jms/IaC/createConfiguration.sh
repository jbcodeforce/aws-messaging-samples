if [ -z "$1" ]
then
    echo "No argument supplied"
    export CFG_NAME=j9r-demo-configuration
else
    echo "Argument: $1"
    export CFG_NAME=$1
fi
echo $CFG_NAME
export CONFIG_ID=$(aws mq list-configurations | jq  --arg name $CFG_NAME -r '.Configurations[] | select (.Name == $name) | .Id' | cut -d ' ' -f 1)

if [ -z "$CONFIG_ID" ]
then
    echo "Configuration not found"
    aws mq create-configuration  --name j9r-demo-configuration \
    --engine-type ACTIVEMQ  \
    --engine-version 5.17.6 \
    --authentication-strategy SIMPLE \
   --tags Environment=dev,SolutionName=amq-demo 
else
    echo "Configuration found"
    echo $CONFIG_ID
fi
