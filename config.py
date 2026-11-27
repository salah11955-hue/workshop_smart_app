import json
from pathlib import Path


BASE_DIR = Path(__file__).resolve().parent
CONFIG_PATH = BASE_DIR / "config.json"


def load_config():
    with CONFIG_PATH.open("r", encoding="utf-8") as config_file:
        return json.load(config_file)


def get_database_path():
    config = load_config()
    database_name = config["database"]["name"]
    database_path = Path(database_name)
    if not database_path.is_absolute():
        database_path = BASE_DIR / database_path
    return str(database_path)
