# -*- coding: utf-8 -*-
import json
import os

_DATA_PATH = os.path.join(os.path.dirname(__file__), 'data_units.json')
with open(_DATA_PATH, 'r', encoding='utf-8') as _f:
    _raw_data = json.load(_f)

UNITS_DATA = {int(k): v for k, v in _raw_data.items()}
