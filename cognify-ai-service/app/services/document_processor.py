"""
Document Processor Service for Cognify AI.
Handles text extraction from PDF, DOCX, TXT, and WhatsApp chat exports.
"""
import re
import logging
from pathlib import Path

logger = logging.getLogger(__name__)


class DocumentProcessor:
    """
    Extracts text content from various document formats.
    Supports: PDF (.pdf), Word (.docx), Plain Text (.txt), WhatsApp exports (.txt)
    """

    SUPPORTED_TYPES = ['PDF', 'DOCX', 'TXT']

    def process_document(self, file_path: str, document_type: str) -> tuple[str, dict]:
        """
        Process a document and extract its text content.

        Args:
            file_path: Path to the document file
            document_type: Type of document (PDF, DOCX, TXT)

        Returns:
            Tuple of (full_text, page_map) where page_map maps character offsets to page numbers
        """
        path = Path(file_path)
        if not path.exists():
            raise FileNotFoundError(f"File not found: {file_path}")

        document_type = document_type.upper()

        if document_type == 'PDF':
            return self._process_pdf(file_path)
        elif document_type == 'DOCX':
            return self._process_docx(file_path)
        elif document_type == 'TXT':
            return self._process_txt(file_path)
        else:
            raise ValueError(f"Unsupported document type: {document_type}")

    def _process_pdf(self, file_path: str) -> tuple[str, dict]:
        """Extract text from PDF using pymupdf4llm for high-quality markdown output."""
        try:
            import pymupdf4llm
            md_text = pymupdf4llm.to_markdown(file_path)

            # Build simple page map (approximate: split by form feeds or page markers)
            page_map = {0: 1}
            import fitz
            doc = fitz.open(file_path)
            offset = 0
            for page_num in range(len(doc)):
                page = doc[page_num]
                page_text = page.get_text()
                page_map[offset] = page_num + 1
                offset += len(page_text)
            doc.close()

            logger.info(f"PDF processed: {len(md_text)} characters from {file_path}")
            return md_text, page_map
        except Exception as e:
            logger.error(f"Error processing PDF: {e}")
            raise

    def _process_docx(self, file_path: str) -> tuple[str, dict]:
        """Extract text from Word documents using python-docx."""
        try:
            from docx import Document
            doc = Document(file_path)

            text = ""
            for para in doc.paragraphs:
                text += para.text + "\n"

            page_map = {0: 1}
            logger.info(f"DOCX processed: {len(text)} characters from {file_path}")
            return text, page_map
        except Exception as e:
            logger.error(f"Error processing DOCX: {e}")
            raise

    def _process_txt(self, file_path: str) -> tuple[str, dict]:
        """
        Process plain text files.
        Auto-detects WhatsApp chat exports and strips timestamps/sender names.
        """
        try:
            with open(file_path, 'r', encoding='utf-8', errors='replace') as f:
                content = f.read()

            # Detect WhatsApp format and extract messages
            if self._is_whatsapp_export(content):
                logger.info("WhatsApp chat export detected, stripping timestamps and sender names")
                content = self._process_whatsapp(content)

            page_map = {0: 1}
            logger.info(f"TXT processed: {len(content)} characters from {file_path}")
            return content, page_map
        except Exception as e:
            logger.error(f"Error processing TXT: {e}")
            raise

    def _is_whatsapp_export(self, text: str) -> bool:
        """
        Detect if a text file is a WhatsApp chat export.
        Looks for common WhatsApp timestamp patterns at the start of lines.
        """
        # Common WhatsApp formats:
        # [DD/MM/YYYY, HH:MM:SS] Name: Message
        # DD/MM/YYYY, HH:MM - Name: Message
        # [M/D/YY, H:MM:SS PM] Name: Message
        whatsapp_patterns = [
            r'^\[\d{1,2}/\d{1,2}/\d{2,4},\s*\d{1,2}:\d{2}(:\d{2})?\s*(AM|PM)?\]',
            r'^\d{1,2}/\d{1,2}/\d{2,4},\s*\d{1,2}:\d{2}\s*-\s*',
            r'^\[\d{1,2}/\d{1,2}/\d{2,4},\s*\d{1,2}:\d{2}:\d{2}\]',
        ]

        lines = text.strip().split('\n')[:20]  # Check first 20 lines
        match_count = 0
        for line in lines:
            for pattern in whatsapp_patterns:
                if re.match(pattern, line.strip()):
                    match_count += 1
                    break

        # If more than 50% of non-empty lines match, it's likely WhatsApp
        non_empty = sum(1 for l in lines if l.strip())
        return non_empty > 0 and (match_count / non_empty) > 0.3

    def _process_whatsapp(self, text: str) -> str:
        """
        Process WhatsApp chat export as per FYP spec:
        Strip timestamps and sender names, keep only message content.
        """
        messages = []

        # Pattern to match WhatsApp message lines and extract just the message body
        # Handles: [DD/MM/YYYY, HH:MM:SS] Name: Message
        # Handles: DD/MM/YYYY, HH:MM - Name: Message
        patterns = [
            # [DD/MM/YYYY, HH:MM:SS] Name: Message
            r'^\[\d{1,2}/\d{1,2}/\d{2,4},\s*\d{1,2}:\d{2}(?::\d{2})?\s*(?:AM|PM)?\]\s*[^:]+:\s*(.+)$',
            # DD/MM/YYYY, HH:MM - Name: Message
            r'^\d{1,2}/\d{1,2}/\d{2,4},\s*\d{1,2}:\d{2}\s*-\s*[^:]+:\s*(.+)$',
        ]

        for line in text.split('\n'):
            line = line.strip()
            if not line:
                continue

            extracted = False
            for pattern in patterns:
                match = re.match(pattern, line)
                if match:
                    message = match.group(1).strip()
                    # Skip system messages (e.g., "Messages and calls are end-to-end encrypted")
                    if message and not message.startswith('<') and not message.endswith('>'):
                        messages.append(message)
                    extracted = True
                    break

            # If no pattern matched, it might be a continuation of a previous message
            if not extracted and messages:
                messages[-1] += " " + line

        return "\n".join(messages)
