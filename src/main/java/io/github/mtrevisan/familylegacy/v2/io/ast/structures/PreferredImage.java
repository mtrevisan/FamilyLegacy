package io.github.mtrevisan.familylegacy.v2.io.ast.structures;

import java.util.Optional;


public record PreferredImage(String uri, Optional<CropCoord> crop){}
