#!/bin/bash
source .service-order.conf
for svc in "${SERVICE_DEPENDENCY_ORDER[@]}"; do
    jar_path=$(find ~/webapps/$svc/lib -name "*.jar" | head -n 1)
    if [ -n "$jar_path" ]; then
        echo "Starting $svc..."
        nohup java -jar "$jar_path" > ~/webapps/$svc/$svc.log 2>&1 &
    fi
done
echo "All services started."
