@echo off
cd /d "%~dp0"
if not exist ai_study_service\.venv (
  py -3 -m venv ai_study_service\.venv
)
call ai_study_service\.venv\Scripts\activate
python -m pip install --upgrade pip
pip install -r ai_study_service\requirements.txt
if "%OPENAI_API_KEY%"=="" (
  echo.
  echo Set your OpenAI API key first:
  echo set OPENAI_API_KEY=your_api_key
  echo Then run this file again.
  pause
  exit /b 1
)
python -m uvicorn main:app --app-dir ai_study_service --host 127.0.0.1 --port 8000
