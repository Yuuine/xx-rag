#!/bin/bash
# Rerank Service Startup Script
# This script will automatically download the model if not present

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "============================================"
echo "Python Rerank Microservice"
echo "============================================"
echo ""

# Check Python
if ! command -v python3 &> /dev/null; then
    echo "Error: Python3 not found! Please install Python 3.10+ first."
    exit 1
fi

echo "Python version:"
python3 --version
echo ""

# Create virtual environment if not exists
if [ ! -d "venv" ]; then
    echo "Creating virtual environment..."
    python3 -m venv venv
fi

# Activate virtual environment
source venv/bin/activate

# Install dependencies
echo "Installing dependencies..."
pip install -r requirements.txt -q

echo ""
echo "Starting Rerank service on port 8082..."
echo "Model will be downloaded automatically on first run if not present."
echo ""

python main.py
