#!/usr/bin/env python3
"""
Generate test DPI (Documento Personal de Identificación) images for patients.
These are placeholder images for testing the file upload functionality.

Usage:
    source .venv/bin/activate
    python scripts/generate_test_dpi_images.py
"""

import os
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

# Patient data extracted from R__seed_test_data.sql
PATIENTS = [
    {"id": 1, "first_name": "Juan", "last_name": "Perez Gonzalez", "dpi": "DPI-1234567890101", "sex": "M"},
    {"id": 2, "first_name": "Maria", "last_name": "Santos Lopez", "dpi": "DPI-2345678901202", "sex": "F"},
    {"id": 3, "first_name": "Pedro", "last_name": "Garcia Hernandez", "dpi": "DPI-3456789012303", "sex": "M"},
    {"id": 4, "first_name": "Ana", "last_name": "Martinez Ruiz", "dpi": "DPI-4567890123404", "sex": "F"},
    {"id": 5, "first_name": "Luis", "last_name": "Morales Castro", "dpi": "DPI-5678901234505", "sex": "M"},
    {"id": 6, "first_name": "Carmen", "last_name": "Flores Mejia", "dpi": "DPI-6789012345606", "sex": "F"},
    {"id": 7, "first_name": "Roberto", "last_name": "Diaz Vargas", "dpi": "DPI-7890123456707", "sex": "M"},
    {"id": 8, "first_name": "Sofia", "last_name": "Ramirez Paz", "dpi": "DPI-8901234567808", "sex": "F"},
    {"id": 9, "first_name": "Miguel", "last_name": "Torres Luna", "dpi": "DPI-9012345678909", "sex": "M"},
    {"id": 10, "first_name": "Elena", "last_name": "Sanchez Rivas", "dpi": "DPI-0123456789010", "sex": "F"},
    {"id": 11, "first_name": "Francisco", "last_name": "Mendoza Aguilar", "dpi": "DPI-1234509876111", "sex": "M"},
    {"id": 12, "first_name": "Isabella", "last_name": "Cruz Monzon", "dpi": "DPI-2345610987212", "sex": "F"},
    {"id": 13, "first_name": "Andres", "last_name": "Ortiz Barrios", "dpi": "DPI-3456721098313", "sex": "M"},
    {"id": 14, "first_name": "Gabriela", "last_name": "Reyes Soto", "dpi": "DPI-4567832109414", "sex": "F"},
    {"id": 15, "first_name": "Oscar", "last_name": "Vasquez Pineda", "dpi": "DPI-5678943210515", "sex": "M"},
    {"id": 16, "first_name": "Patricia", "last_name": "Herrera Godinez", "dpi": "DPI-6789054321616", "sex": "F"},
    {"id": 17, "first_name": "Diego", "last_name": "Castillo Moreno", "dpi": "DPI-7890165432717", "sex": "M"},
    {"id": 18, "first_name": "Valentina", "last_name": "Estrada Juarez", "dpi": "DPI-8901276543818", "sex": "F"},
    {"id": 19, "first_name": "Alejandro", "last_name": "Nunez Cordova", "dpi": "DPI-9012387654919", "sex": "M"},
    {"id": 20, "first_name": "Lucia", "last_name": "Alvarez Monroy", "dpi": "DPI-0123498765020", "sex": "F"},
]

# Colors (Guatemala flag colors)
BLUE = (0, 63, 135)  # Azul Maya
WHITE = (255, 255, 255)
LIGHT_BLUE = (130, 170, 200)
GRAY = (128, 128, 128)
DARK_GRAY = (64, 64, 64)
LIGHT_GRAY = (240, 240, 240)


def create_dpi_image(patient: dict, output_dir: Path) -> str:
    """
    Create a simulated DPI image for a patient.

    The image is a simple representation of a Guatemalan ID card with:
    - Blue header with "DOCUMENTO PERSONAL DE IDENTIFICACIÓN"
    - Patient photo placeholder
    - Patient name and DPI number
    - "MUESTRA - NO VÁLIDO" watermark
    """
    # ID card dimensions (standard credit card ratio, scaled up)
    width, height = 600, 380

    # Create image with white background
    img = Image.new('RGB', (width, height), WHITE)
    draw = ImageDraw.Draw(img)

    # Try to load a font, fallback to default if not available
    try:
        title_font = ImageFont.truetype("/System/Library/Fonts/Helvetica.ttc", 18)
        header_font = ImageFont.truetype("/System/Library/Fonts/Helvetica.ttc", 14)
        name_font = ImageFont.truetype("/System/Library/Fonts/Helvetica.ttc", 16)
        label_font = ImageFont.truetype("/System/Library/Fonts/Helvetica.ttc", 12)
        dpi_font = ImageFont.truetype("/System/Library/Fonts/Helvetica.ttc", 20)
        watermark_font = ImageFont.truetype("/System/Library/Fonts/Helvetica.ttc", 28)
    except (IOError, OSError):
        title_font = ImageFont.load_default()
        header_font = title_font
        name_font = title_font
        label_font = title_font
        dpi_font = title_font
        watermark_font = title_font

    # Draw blue header bar
    draw.rectangle([0, 0, width, 50], fill=BLUE)
    draw.text((width // 2, 15), "DOCUMENTO PERSONAL DE IDENTIFICACIÓN",
              fill=WHITE, font=title_font, anchor="mt")
    draw.text((width // 2, 35), "REPÚBLICA DE GUATEMALA",
              fill=LIGHT_BLUE, font=header_font, anchor="mt")

    # Draw photo placeholder (left side)
    photo_x, photo_y = 30, 70
    photo_w, photo_h = 140, 180
    draw.rectangle([photo_x, photo_y, photo_x + photo_w, photo_y + photo_h],
                   fill=LIGHT_GRAY, outline=GRAY)

    # Draw silhouette in photo area
    center_x = photo_x + photo_w // 2
    # Head circle
    head_radius = 30
    head_y = photo_y + 50
    draw.ellipse([center_x - head_radius, head_y - head_radius,
                  center_x + head_radius, head_y + head_radius], fill=GRAY)
    # Body arc
    body_top = head_y + head_radius + 10
    draw.ellipse([center_x - 50, body_top, center_x + 50, photo_y + photo_h + 20], fill=GRAY)

    # Draw text fields (right side)
    text_x = 190
    line_height = 28

    # Name
    y = 80
    draw.text((text_x, y), "NOMBRES:", fill=GRAY, font=label_font)
    y += 15
    draw.text((text_x, y), patient["first_name"].upper(), fill=DARK_GRAY, font=name_font)

    # Last name
    y += line_height + 10
    draw.text((text_x, y), "APELLIDOS:", fill=GRAY, font=label_font)
    y += 15
    draw.text((text_x, y), patient["last_name"].upper(), fill=DARK_GRAY, font=name_font)

    # CUI/DPI Number
    y += line_height + 15
    draw.text((text_x, y), "CUI:", fill=GRAY, font=label_font)
    y += 15
    # Format the DPI number nicely (remove "DPI-" prefix for display)
    dpi_number = patient["dpi"].replace("DPI-", "")
    # Format as XXXX XXXXX XXXX
    formatted_dpi = f"{dpi_number[:4]} {dpi_number[4:9]} {dpi_number[9:]}"
    draw.text((text_x, y), formatted_dpi, fill=BLUE, font=dpi_font)

    # Sex
    y += line_height + 10
    draw.text((text_x, y), "SEXO:", fill=GRAY, font=label_font)
    sex_text = "MASCULINO" if patient["sex"] == "M" else "FEMENINO"
    draw.text((text_x + 50, y), sex_text, fill=DARK_GRAY, font=label_font)

    # Draw blue footer bar
    draw.rectangle([0, height - 40, width, height], fill=BLUE)
    draw.text((width // 2, height - 25), "REGISTRO NACIONAL DE LAS PERSONAS - RENAP",
              fill=WHITE, font=label_font, anchor="mt")

    # Draw diagonal "MUESTRA - NO VÁLIDO" watermark
    watermark_img = Image.new('RGBA', (width, height), (255, 255, 255, 0))
    watermark_draw = ImageDraw.Draw(watermark_img)

    # Rotate the watermark text
    watermark_text = "MUESTRA - NO VÁLIDO"
    watermark_draw.text((width // 2, height // 2), watermark_text,
                        fill=(255, 0, 0, 80), font=watermark_font, anchor="mm")

    # Convert main image to RGBA for compositing
    img = img.convert('RGBA')

    # Rotate watermark and composite
    rotated = watermark_img.rotate(-30, expand=False, center=(width // 2, height // 2))
    img = Image.alpha_composite(img, rotated)

    # Convert back to RGB for JPEG saving
    img = img.convert('RGB')

    # Draw border
    draw = ImageDraw.Draw(img)
    draw.rectangle([0, 0, width - 1, height - 1], outline=BLUE, width=2)

    # Save image
    filename = f"dpi_{patient['first_name'].lower()}_{patient['last_name'].split()[0].lower()}.jpg"
    filepath = output_dir / filename
    img.save(filepath, 'JPEG', quality=90)

    return str(filepath)


def main():
    # Determine output directory
    script_dir = Path(__file__).parent.parent
    output_dir = script_dir / "api" / "data" / "test-dpi-images"
    output_dir.mkdir(parents=True, exist_ok=True)

    print(f"Generating {len(PATIENTS)} test DPI images...")
    print(f"Output directory: {output_dir}")
    print()

    for patient in PATIENTS:
        filepath = create_dpi_image(patient, output_dir)
        print(f"  Created: {Path(filepath).name}")

    print()
    print(f"Done! Generated {len(PATIENTS)} images in {output_dir}")
    print()
    print("You can now upload these images to test the patient ID document upload functionality.")


if __name__ == "__main__":
    main()
