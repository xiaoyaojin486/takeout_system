# =====================================================================
#  外卖系统后端镜像（多阶段：Maven 编译 → JRE 运行）
#  构建（在项目根目录执行）：
#    docker build -t takeout-server:1.0.0 backend/
# =====================================================================

# ===== Stage 1：Maven 编译打包 =====
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

# 先只拷 pom.xml 单独拉依赖：依赖不变时这一层命中缓存，源码改动不会触发重新下载
COPY pom.xml .
RUN mvn -B -q dependency:go-offline || true

# 再拷源码编译。跳过测试：测试需要真实 MySQL，放在 CI/本地跑，不打进镜像构建
COPY src ./src
RUN mvn -B clean package -DskipTests \
 && cp target/takeout-server-*.jar /build/app.jar

# ===== Stage 2：JRE 运行时 =====
FROM eclipse-temurin:17-jre
WORKDIR /app

ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

COPY --from=builder /build/app.jar app.jar

EXPOSE 8080
# -Xmx512m：目标机内存有限，堆上限压到 512m，避免拖垮同机其它容器
ENTRYPOINT ["java", "-Xmx512m", "-jar", "app.jar"]