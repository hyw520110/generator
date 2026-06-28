apiVersion: apps/v1
kind: Deployment
metadata:
  name: ${projectName}-deployment
  labels:
    app: ${projectName}
spec:
  replicas: 1
  selector:
    matchLabels:
      app: ${projectName}
  template:
    metadata:
      labels:
        app: ${projectName}
    spec:
      containers:
      - name: ${projectName}
        image: ${projectName}:latest
        ports:
        - containerPort: 8080
---
apiVersion: v1
kind: Service
metadata:
  name: ${projectName}-service
spec:
  selector:
    app: ${projectName}
  ports:
    - protocol: TCP
      port: 80
      targetPort: 8080
  type: ClusterIP
