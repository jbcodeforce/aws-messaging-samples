if [ -z "$1" ]
then
    echo "No argument supplied, specify a Configuration ID"
else
    echo "Argument: $1"
    export CFG_ID=$1
fi

export CONFIG_ID=$(aws mq list-configurations | jq  --arg name $CFG_NAME -r '.Configurations[] | select (.Id == $name)')

if [ -z "$CONFIG_ID" ]
then
    echo "Configuration not found"
else
    echo "Configuration found"
    echo $CONFIG_ID
fi

# base64 of the broker.xml
base64 -i ../config/broker-config.xml -o t.b
aws mq update-configuration --configuration-id $CONFIG_ID --data file//./t.b
rm t.b