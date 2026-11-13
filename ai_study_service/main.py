import os
from pathlib import Path
from typing import Literal

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from openai import OpenAI
from pypdf import PdfReader
from docx import Document as WordDocument
from pptx import Presentation

APP_ROOT = Path(__file__).resolve().parent.parent
UPLOAD_ROOT = (APP_ROOT / "uploads" / "documents").resolve()
MAX_CHARS = 45000

app = FastAPI(title="UniShare AI Study Material Service", version="1.0.0")


class GenerateRequest(BaseModel):
    file_path: str = Field(min_length=1, max_length=500)
    material_type: Literal[
        "MCQs with answers",
        "Flashcards",
        "Important questions",
        "Short and long-answer questions",
        "Chapter-wise revision notes",
    ]


def safe_document_path(relative_path: str) -> Path:
    candidate = (APP_ROOT / relative_path).resolve()
    if not candidate.is_relative_to(UPLOAD_ROOT) or not candidate.is_file():
        raise HTTPException(status_code=400, detail="Document path is invalid or unavailable.")
    return candidate


def extract_text(path: Path) -> str:
    suffix = path.suffix.lower()
    try:
        if suffix == ".pdf":
            reader = PdfReader(str(path))
            parts = [page.extract_text() or "" for page in reader.pages]
            text = "\n".join(parts)
        elif suffix == ".docx":
            doc = WordDocument(str(path))
            text = "\n".join(p.text for p in doc.paragraphs)
            for table in doc.tables:
                for row in table.rows:
                    text += "\n" + " | ".join(cell.text for cell in row.cells)
        elif suffix == ".pptx":
            presentation = Presentation(str(path))
            slides = []
            for number, slide in enumerate(presentation.slides, start=1):
                lines = [shape.text for shape in slide.shapes if getattr(shape, "has_text_frame", False)]
                slides.append(f"Slide {number}\n" + "\n".join(lines))
            text = "\n\n".join(slides)
        elif suffix == ".txt":
            text = path.read_text(encoding="utf-8", errors="replace")
        else:
            raise HTTPException(
                status_code=415,
                detail="AI text extraction currently supports PDF, DOCX, PPTX and TXT. Convert legacy DOC/PPT files first."
            )
    except HTTPException:
        raise
    except Exception as exc:
        raise HTTPException(status_code=422, detail=f"Could not extract document text: {exc}")

    text = text.strip()
    if len(text) < 80:
        raise HTTPException(
            status_code=422,
            detail="Not enough selectable text was found. Scanned PDFs need OCR before study materials can be generated."
        )
    return text[:MAX_CHARS]


PROMPTS = {
    "MCQs with answers": (
        "Create 15 high-quality multiple-choice questions from the source. Each must have four options "
        "(A-D), exactly one correct answer, and a brief explanation. Include difficulty labels."
    ),
    "Flashcards": (
        "Create 20 concise question-and-answer flashcards for active recall. Cover definitions, concepts, "
        "comparisons, formulas and processes present in the source."
    ),
    "Important questions": (
        "Identify 15 important exam-oriented questions. Group them by topic and explain briefly why each "
        "topic/question matters based on emphasis and coverage in the source."
    ),
    "Short and long-answer questions": (
        "Create 8 short-answer and 6 long-answer questions. Provide model answers using only the source. "
        "Keep short answers concise and structure long answers with headings and key points."
    ),
    "Chapter-wise revision notes": (
        "Create structured revision notes organized by the chapter/section headings found in the source. "
        "For each chapter include key concepts, definitions, processes, formulas (if present), and a quick recap."
    ),
}


@app.get("/health")
def health():
    return {"status": "ok", "service": "UniShare AI Study Materials"}


@app.post("/generate")
def generate(request: GenerateRequest):
    api_key = os.getenv("OPENAI_API_KEY")
    if not api_key:
        raise HTTPException(status_code=503, detail="OPENAI_API_KEY is not configured on this computer.")

    path = safe_document_path(request.file_path)
    source_text = extract_text(path)
    client = OpenAI(api_key=api_key)
    try:
        response = client.responses.create(
            model=os.getenv("OPENAI_MODEL", "gpt-4.1-mini"),
            instructions=(
                "You are UniShare's source-grounded academic study assistant. Use only the supplied document. "
                "Do not invent facts, formulas, chapter names, page references or answers. If the source lacks "
                "enough information, clearly say so. Preserve the document's terminology and level. Format "
                "the output in clean Markdown. State that generated material should be checked against the source."
            ),
            input=f"Requested output: {request.material_type}\n\n{PROMPTS[request.material_type]}\n\n"
                  f"DOCUMENT CONTENT:\n{source_text}",
        )
        return {"material_type": request.material_type, "content": response.output_text}
    except Exception as exc:
        raise HTTPException(status_code=502, detail=f"AI generation failed: {exc}")
