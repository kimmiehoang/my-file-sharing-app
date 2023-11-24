#!/bin/bash

# Kiểm tra số lượng tham số đầu vào
if [ "$#" -eq 0 ]; then
    echo "Usage: $0 <command> [args...]"
    exit 1
fi

# Lấy tham số command từ đối số dòng lệnh
command=$1

# Lấy các tham số còn lại từ đối số dòng lệnh
shift

# Chạy chương trình Java Client với tham số command và các tham số còn lại
java Client "$command" "$@"