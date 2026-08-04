#!/bin/bash

# 智慧旅游系统 - Linux虚拟机部署脚本
# 适用于CentOS 7/8, Ubuntu 20.04/22.04

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检测操作系统
detect_os() {
    if [ -f /etc/os-release ]; then
        . /etc/os-release
        OS=$NAME
        VERSION=$VERSION_ID
    else
        log_error "无法检测操作系统"
        exit 1
    fi
    log_info "检测到操作系统: $OS $VERSION"
}

# 安装基础依赖
install_base_deps() {
    log_info "安装基础依赖..."
    
    if [[ "$OS" == *"CentOS"* ]] || [[ "$OS" == *"Red Hat"* ]]; then
        sudo yum update -y
        sudo yum install -y wget curl vim net-tools telnet lsof unzip
        sudo yum install -y epel-release
    elif [[ "$OS" == *"Ubuntu"* ]] || [[ "$OS" == *"Debian"* ]]; then
        sudo apt-get update
        sudo apt-get install -y wget curl vim net-tools telnet lsof unzip
    fi
    
    log_info "基础依赖安装完成"
}

# 安装JDK 17
install_jdk() {
    log_info "安装JDK 17..."
    
    if [[ "$OS" == *"CentOS"* ]] || [[ "$OS" == *"Red Hat"* ]]; then
        sudo yum install -y java-17-openjdk java-17-openjdk-devel
    elif [[ "$OS" == *"Ubuntu"* ]] || [[ "$OS" == *"Debian"* ]]; then
        sudo apt-get install -y openjdk-17-jdk
    fi
    
    # 配置环境变量
    if ! grep -q "JAVA_HOME" ~/.bashrc; then
        echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk' >> ~/.bashrc
        echo 'export PATH=$PATH:$JAVA_HOME/bin' >> ~/.bashrc
        source ~/.bashrc
    fi
    
    java -version
    log_info "JDK 17安装完成"
}

# 安装MySQL
install_mysql() {
    log_info "安装MySQL 8.0..."
    
    if [[ "$OS" == *"CentOS"* ]] || [[ "$OS" == *"Red Hat"* ]]; then
        sudo rpm -Uvh https://repo.mysql.com//mysql80-community-release-el7-11.noarch.rpm
        sudo yum install -y mysql-community-server
        sudo systemctl enable mysqld
        sudo systemctl start mysqld
    elif [[ "$OS" == *"Ubuntu"* ]] || [[ "$OS" == *"Debian"* ]]; then
        sudo apt-get install -y mysql-server-8.0
        sudo systemctl enable mysql
        sudo systemctl start mysql
    fi
    
    # 获取临时密码
    TEMP_PASS=$(sudo grep 'temporary password' /var/log/mysqld.log | awk '{print $NF}')
    log_warn "MySQL临时密码: $TEMP_PASS"
    log_warn "请运行: sudo mysql_secure_installation 进行安全配置"
    
    log_info "MySQL安装完成"
}

# 安装Redis
install_redis() {
    log_info "安装Redis..."
    
    if [[ "$OS" == *"CentOS"* ]] || [[ "$OS" == *"Red Hat"* ]]; then
        sudo yum install -y redis
        sudo systemctl enable redis
        sudo systemctl start redis
    elif [[ "$OS" == *"Ubuntu"* ]] || [[ "$OS" == *"Debian"* ]]; then
        sudo apt-get install -y redis-server
        sudo systemctl enable redis
        sudo systemctl start redis
    fi
    
    # 配置Redis密码
    sudo sed -i 's/^# requirepass/requirepass/' /etc/redis.conf
    REDIS_PASSWORD=${REDIS_PASSWORD:-change-me}
    sudo sed -i "s/^requirepass .*/requirepass ${REDIS_PASSWORD}/" /etc/redis.conf
    sudo systemctl restart redis
    
    log_info "Redis安装完成"
}

# 安装Nginx
install_nginx() {
    log_info "安装Nginx..."
    
    if [[ "$OS" == *"CentOS"* ]] || [[ "$OS" == *"Red Hat"* ]]; then
        sudo yum install -y nginx
        sudo systemctl enable nginx
        sudo systemctl start nginx
    elif [[ "$OS" == *"Ubuntu"* ]] || [[ "$OS" == *"Debian"* ]]; then
        sudo apt-get install -y nginx
        sudo systemctl enable nginx
        sudo systemctl start nginx
    fi
    
    log_info "Nginx安装完成"
}

# 安装Docker
install_docker() {
    log_info "安装Docker..."
    
    if [[ "$OS" == *"CentOS"* ]] || [[ "$OS" == *"Red Hat"* ]]; then
        sudo yum install -y yum-utils
        sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
        sudo yum install -y docker-ce docker-ce-cli containerd.io
    elif [[ "$OS" == *"Ubuntu"* ]] || [[ "$OS" == *"Debian"* ]]; then
        sudo apt-get install -y apt-transport-https ca-certificates curl gnupg lsb-release
        curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
        echo "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
        sudo apt-get update
        sudo apt-get install -y docker-ce docker-ce-cli containerd.io
    fi
    
    sudo systemctl enable docker
    sudo systemctl start docker
    sudo usermod -aG docker $USER
    
    log_info "Docker安装完成"
}

# 创建应用目录
setup_app_dirs() {
    log_info "创建应用目录..."
    
    sudo mkdir -p /opt/travel-app
    sudo mkdir -p /opt/travel-app/logs
    sudo mkdir -p /opt/travel-app/config
    sudo mkdir -p /opt/travel-app/backup
    
    sudo chown -R $USER:$USER /opt/travel-app
    
    log_info "应用目录创建完成"
}

# 配置防火墙
configure_firewall() {
    log_info "配置防火墙..."
    
    if command -v firewall-cmd &> /dev/null; then
        sudo firewall-cmd --permanent --add-port=8080/tcp
        sudo firewall-cmd --permanent --add-port=3306/tcp
        sudo firewall-cmd --permanent --add-port=6379/tcp
        sudo firewall-cmd --permanent --add-port=80/tcp
        sudo firewall-cmd --permanent --add-port=443/tcp
        sudo firewall-cmd --reload
    elif command -v ufw &> /dev/null; then
        sudo ufw allow 8080/tcp
        sudo ufw allow 3306/tcp
        sudo ufw allow 6379/tcp
        sudo ufw allow 80/tcp
        sudo ufw allow 443/tcp
        sudo ufw --force enable
    fi
    
    log_info "防火墙配置完成"
}

# 创建系统服务
create_systemd_service() {
    log_info "创建系统服务..."
    
    sudo tee /etc/systemd/system/travel-app.service > /dev/null << EOF
[Unit]
Description=智慧旅游系统
After=network.target mysql.service redis.service

[Service]
Type=simple
User=$USER
WorkingDirectory=/opt/travel-app
ExecStart=/usr/bin/java -jar -Xms512m -Xmx1024m -Dspring.profiles.active=prod /opt/travel-app/travel-app.jar
ExecStop=/bin/kill -15 \$MAINPID
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

    sudo systemctl daemon-reload
    sudo systemctl enable travel-app
    
    log_info "系统服务创建完成"
}

# 主函数
main() {
    log_info "开始部署智慧旅游系统..."
    
    detect_os
    install_base_deps
    install_jdk
    install_mysql
    install_redis
    install_nginx
    install_docker
    setup_app_dirs
    configure_firewall
    create_systemd_service
    
    log_info "======================================"
    log_info "部署脚本执行完成！"
    log_info "======================================"
    log_info "请完成以下步骤："
    log_info "1. 配置MySQL: sudo mysql_secure_installation"
    log_info "2. 创建数据库: mysql -u root -p < init.sql"
    log_info "3. 上传应用jar包到 /opt/travel-app/"
    log_info "4. 启动服务: sudo systemctl start travel-app"
    log_info "======================================"
}

# 执行主函数
main
