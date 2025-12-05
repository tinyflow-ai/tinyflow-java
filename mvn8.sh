 #!/bin/bash

# 设置 JDK 8 的 JAVA_HOME（自动获取）
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-1.8.jdk/Contents/Home

# 显式指定要构建的模块（跳过 springai 和 langchain4j）
 mvn \
  -pl tinyflow-core,tinyflow-node,tinyflow-support-agentsflex,tinyflow-support-solonai \
  -am \
  "$@"