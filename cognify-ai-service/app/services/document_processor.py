"""
Local document processing - PDF/DOCX/TXT/WHATSAPP parsing
"""
import logging
from typing import List, Tuple
import pymupdf4llm
from docx import Document as DocxDocument
from whatstk import WhatsAppChat

logger = logging.getLogger(__name__)

class DocumentProcessor:
    def process_document(self, file_path: str, document_type: str) -> Tuple[str, List[int]]:
        """Extract text with basic page number mapping where applicable"""
        logger.info(f"Processing {document_type}: {file_path}")
        
        if document_type == "PDF":
            return self._process_pdf(file_path)
        elif document_type == "DOCX":
            return self._process_docx(file_path)
        elif document_type == "WHATSAPP":
            return self._process_whatsapp(file_path)
        elif document_type == "TXT":
            return self._process_txt(file_path)
        else:
            raise ValueError(f"Unsupported type: {document_type}")
    
    def _process_pdf(self, file_path: str) -> Tuple[str, List[int]]:
        """Parse PDF using pymupdf4llm which brings superior markdown output tailored for LLMs"""
        try:
            # pymupdf4llm.to_markdown returns rich text, great for RAG structure
            md_text = pymupdf4llm.to_markdown(file_path)
            # We map the whole document to page 1 for simplicity in this upgraded pipeline
            # as chunking service will just take the markdown stream.
            page_map = [1] * len(md_text)
            
            logger.info(f"Extracted {len(md_text)} chars using pymupdf4llm")
            return md_text, page_map
        except Exception as e:
            logger.error(f"PDF processing error (pymupdf4llm): {e}")
            raise
    
    def _process_docx(self, file_path: str) -> Tuple[str, List[int]]:
        """Parse DOCX"""
        try:
            doc = DocxDocument(file_path)
            full_text = ""
            current_page = 1
            words_in_page = 0
            page_map = []
            
            for para in doc.paragraphs:
                text = para.text + "\\n"
                words = len(text.split())
                
                # Estimate page break (~500 words/page)
                if words_in_page + words > 500:
                    current_page += 1
                    words_in_page = 0
                
                words_in_page += words
                page_map.extend([current_page] * len(text))
                full_text += text
            
            logger.info(f"Extracted {len(full_text)} chars (~{current_page} pages)")
            return full_text, page_map
        except Exception as e:
            logger.error(f"DOCX processing error: {e}")
            raise
            
    def _process_whatsapp(self, file_path: str) -> Tuple[str, List[int]]:
        """Parse WhatsApp Chat export using whatstk"""
        try:
            chat = WhatsAppChat.from_source(filepath=file_path)
            df = chat.df
            
            # Convert dataframe to a readable text format for RAG
            full_text = ""
            for index, row in df.iterrows():
                # 'date', 'username', 'message' are the standard columns produced by whatstk
                line = f"[{row['date']}] {row['username']}: {row['message']}\\n"
                full_text += line
                
            page_map = [1] * len(full_text)
            logger.info(f"Extracted {len(full_text)} chars from WhatsApp chat")
            return full_text, page_map
        except Exception as e:
            logger.error(f"WhatsApp processing error (whatstk): {e}")
            # Fallback to standard TXT parsing if whatstk fails due to weird formats
            return self._process_txt(file_path)
    
    def _process_txt(self, file_path: str) -> Tuple[str, List[int]]:
        """Parse basic TXT file"""
        try:
            with open(file_path, 'r', encoding='utf-8') as file:
                full_text = file.read()
            page_map = [1] * len(full_text)
            logger.info(f"Extracted {len(full_text)} chars")
            return full_text, page_map
        except Exception as e:
            logger.error(f"TXT processing error: {e}")
            raise
